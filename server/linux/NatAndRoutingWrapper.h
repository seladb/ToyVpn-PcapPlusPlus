#pragma once

#include <ostream>
#include "libs/pcapplusplus/include/pcapplusplus/IpAddress.h"
#include "Log.h"

class NatAndRoutingWrapper {
public:
    virtual ~NatAndRoutingWrapper() {
        if (m_IsInitialized) {
            configureNat(false);
        }
    }

    void init(const std::string& publicNetworkInterface, const std::string& tunInterfaceName, const pcpp::IPv4Network& privateNetwork) {
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
        auto enableFlag = enable ? "-A" : "-D";

        std::ostringstream iptablesCommand;
        iptablesCommand
                << "iptables -t nat "
                << enableFlag
                << " POSTROUTING -s "
                << m_PrivateNetwork.toString()
                << " -o "
                << m_PublicNetworkInterface
                << " -j MASQUERADE";

        int result = std::system(iptablesCommand.str().c_str());
        if (result != 0) {
            throw std::runtime_error("Couldn't configure NAT: '" + iptablesCommand.str() + "'");
        }

        if (enable) {
            TOYVPN_LOG_DEBUG("Configuring NAT: '" << iptablesCommand.str() << "'");
        }
        else {
            TOYVPN_LOG_DEBUG("Disabling NAT: '" << iptablesCommand.str() << "'");
        }
    }

    void configureRouting() {
        std::ostringstream ipRouteCommand;
        ipRouteCommand
                << "ip route add "
                << m_PrivateNetwork.toString()
                << " dev "
                << m_TunInterfaceName;

        auto result = std::system(ipRouteCommand.str().c_str());
        if (result != 0) {
            throw std::runtime_error("Couldn't configure roting: '" + ipRouteCommand.str() + "'");
        }
    }
};