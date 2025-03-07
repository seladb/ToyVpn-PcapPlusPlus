#pragma once

#include "Log.h"
#include "libs/pcapplusplus/include/pcapplusplus/IpAddress.h"
#include <ostream>

class IpTablesCommand {
  public:
    IpTablesCommand(const std::string &commandTemplate)
        : m_CommandTemplate(commandTemplate) {}

    void run(bool enable) {
        std::string replacement = enable ? "-A" : "-D";
        std::string command = m_CommandTemplate;

        auto pos = command.find(templateString);
        if (pos != std::string::npos) {
            command.replace(pos, templateString.length(), replacement);
            int result = std::system(command.c_str());
            if (result != 0) {
                throw std::runtime_error("Couldn't configure NAT: '" + command +
                                         "'");
            }

            if (enable) {
                TOYVPN_LOG_DEBUG("Configured iptables: '" << command << "'");
            } else {
                TOYVPN_LOG_DEBUG("Disabled iptables: '" << command << "'");
            }

            m_CommandRun = true;
        }
    }

    void rollback() {
        if (m_CommandRun) {
            run(false);
            m_CommandRun = false;
        }
    }

    constexpr static std::string_view templateString = "{enable}";

  private:
    std::string m_CommandTemplate;
    bool m_CommandRun = false;
};

class NatAndRoutingWrapper {
  public:
    virtual ~NatAndRoutingWrapper() {
        if (m_IsInitialized) {
            configureNat(false);
        }
    }

    void init(const std::string &publicNetworkInterface,
              const std::string &tunInterfaceName,
              const pcpp::IPv4Network &privateNetwork) {
        m_PublicNetworkInterface = publicNetworkInterface;
        m_PrivateNetwork = privateNetwork;
        m_TunInterfaceName = tunInterfaceName;
        configureRouting();
        configureNat(true);
        m_IsInitialized = true;
    }

  private:
    std::string m_PublicNetworkInterface;
    std::string m_TunInterfaceName;
    pcpp::IPv4Network m_PrivateNetwork = std::string("0.0.0.0/0");
    bool m_IsInitialized = false;

    void configureNat(bool enable) {
        std::ostringstream iptablesPostRoutingCommandStream;
        iptablesPostRoutingCommandStream
            << "iptables -t nat " << IpTablesCommand::templateString
            << " POSTROUTING -s " << m_PrivateNetwork.toString() << " -o "
            << m_PublicNetworkInterface << " -j MASQUERADE";
        IpTablesCommand iptablesPostRoutingCommand(
            iptablesPostRoutingCommandStream.str());

        std::ostringstream iptablesForwardCommandStream;
        iptablesForwardCommandStream
            << "iptables"
            << " " << IpTablesCommand::templateString << " FORWARD"
            << " -i " << m_TunInterfaceName << " -o "
            << m_PublicNetworkInterface << " -j ACCEPT";
        IpTablesCommand iptablesForwardCommand(
            iptablesForwardCommandStream.str());

        std::ostringstream iptablesForwardCommandReverseStream;
        iptablesForwardCommandReverseStream
            << "iptables"
            << " " << IpTablesCommand::templateString << " FORWARD"
            << " -i " << m_PublicNetworkInterface << " -o "
            << m_TunInterfaceName << " -j ACCEPT";
        IpTablesCommand iptablesForwardCommandReverse(
            iptablesForwardCommandReverseStream.str());

        try {
            iptablesPostRoutingCommand.run(enable);
            iptablesForwardCommand.run(enable);
            iptablesForwardCommandReverse.run(enable);
        } catch (const std::exception &) {
            iptablesPostRoutingCommand.rollback();
            iptablesForwardCommand.rollback();
            iptablesForwardCommandReverse.rollback();

            throw;
        }
    }

    void configureRouting() {
        std::ostringstream ipRouteCommand;
        ipRouteCommand << "ip route add " << m_PrivateNetwork.toString()
                       << " dev " << m_TunInterfaceName;

        auto result = std::system(ipRouteCommand.str().c_str());
        if (result != 0) {
            throw std::runtime_error("Couldn't configure roting: '" +
                                     ipRouteCommand.str() + "'");
        }
    }
};
