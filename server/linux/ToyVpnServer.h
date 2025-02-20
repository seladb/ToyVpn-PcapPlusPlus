#pragma once

#include <unordered_map>
#include <chrono>
#include "libs/pcapplusplus/include/pcapplusplus/IpAddress.h"
#include "libs/pcapplusplus/include/pcapplusplus/Packet.h"
#include "libs/pcapplusplus/include/pcapplusplus/IPv4Layer.h"
#include "Utils.h"
#include "EpollWrapper.h"
#include "TunInterfaceWrapper.h"
#include "ToyVpnConfiguration.h"
#include "ServerSocketWrapper.h"
#include "IpForwardingWrapper.h"
#include "NatAndRoutingWrapper.h"
#include "ClientHandler.h"
#include "PacketHandler.h"
#include "Log.h"

class ToyVpnServer {
public:
    ToyVpnServer(const ToyVpnConfiguration& config) : m_Config(config)
    {}

    void start() {
        TOYVPN_LOG_INFO("Starting server...");
        m_IpForwarding.init();
        m_TunInterface.init(m_Config.tunInterfaceName, m_Config.privateNetwork);
        m_ServerSocket.init(m_Config.port);
        m_NatAndRouting.init(m_Config.publicNetworkInterface, m_Config.tunInterfaceName, m_Config.privateNetwork);

        m_EpollWrapper.init(10);
        m_EpollWrapper.add(m_ServerSocket.getSocketFd(), [this](int fd) { handleClient(); });
        m_EpollWrapper.add(m_TunInterface.getInterfaceFd(), [this](int fd) {
            handleTunInterface();
        });

        m_LastUsedClientAddress = m_TunInterface.getTunIpAddress();

        m_LastIdleClientsCheck = std::chrono::steady_clock::now();

        if (m_Config.saveFilePath.has_value()) {
            m_PacketHandler.emplace(m_Config.saveFilePath.value());
        }

        m_EpollWrapper.startPolling();
    }

    void stop() {
        TOYVPN_LOG_INFO("Stopping server...");
        for (const auto& item : m_Clients) {
            item.second->disconnect();
        }

        m_EpollWrapper.stopPolling();
        if (m_PacketHandler.has_value()) {
            m_PacketHandler->stop();
        }
        TOYVPN_LOG_INFO("Server stopped");
    }

private:
    constexpr static int m_MaxConnections = 50;
    constexpr static int m_BufferSize = 32767;
    constexpr static std::chrono::duration m_CheckIdleClientsSec = std::chrono::seconds(5);
    constexpr static int m_MaxQueueCapacity = 1000;

    ToyVpnConfiguration m_Config;

    EPollWrapper m_EpollWrapper;
    TunInterfaceWrapper m_TunInterface;
    ServerSocketWrapper m_ServerSocket;
    IpForwardingWrapper m_IpForwarding;
    NatAndRoutingWrapper m_NatAndRouting;

    std::unordered_map<sockaddr_in6, std::shared_ptr<ClientHandler>, sockaddrIn6Hash, sockaddrIn6Equal> m_Clients;
    std::unordered_map<uint32_t, std::shared_ptr<ClientHandler>> m_ClientAddressMap;
    std::array<uint8_t, m_BufferSize> m_Buffer;
    std::chrono::steady_clock::time_point m_LastIdleClientsCheck;
    pcpp::IPv4Address m_LastUsedClientAddress;

    std::optional<PacketHandler> m_PacketHandler;

    void handleClient() {
        sockaddr_in6 clientAddress;
        auto bytesReceived = m_ServerSocket.receive(m_Buffer, clientAddress);
        if (bytesReceived > 0) {
            // New client
            if (m_Clients.find(clientAddress) == m_Clients.end()) {
                auto newClient = std::make_shared<ClientHandler>(m_ServerSocket, clientAddress,
                                                                 m_TunInterface, createVpnSettings(), m_PacketHandler);
                m_Clients[clientAddress] = newClient;
                m_ClientAddressMap[newClient->getClientVpnAddress().toInt()] = newClient;
            }

            m_Clients[clientAddress]->handleDataFromClient(m_Buffer, bytesReceived);
        }

        auto now = std::chrono::steady_clock::now();
        if (now - m_LastIdleClientsCheck > m_CheckIdleClientsSec) {
            checkIdleClients(now);
            m_LastIdleClientsCheck = now;
        }
    }

    void handleTunInterface() {
        size_t bytesReceived = m_TunInterface.receive(m_Buffer);
        if (bytesReceived > 0) {
            timespec ts;
            pcpp::RawPacket rawPacket(m_Buffer.data(), bytesReceived, ts, false, pcpp::LINKTYPE_DLT_RAW1);
            pcpp::Packet packet(&rawPacket);
            if (packet.isPacketOfType(pcpp::IPv4)) {
                auto ipv4Layer = packet.getLayerOfType<pcpp::IPv4Layer>();
                if (auto it = m_ClientAddressMap.find(ipv4Layer->getDstIPv4Address().toInt()); it !=
                        m_ClientAddressMap.end()) {
                    it->second->handleDataFromTun(m_Buffer, bytesReceived);
                }
            }
        }

        auto now = std::chrono::steady_clock::now();
        if (now - m_LastIdleClientsCheck > m_CheckIdleClientsSec) {
            checkIdleClients(now);
            m_LastIdleClientsCheck = now;
        }
    }

    VpnSettings createVpnSettings() {
        auto nextClientAddress = pcpp::IPv4Address(htonl(ntohl(m_LastUsedClientAddress.toInt()) + 1));
        if (nextClientAddress == m_Config.privateNetwork.getHighestAddress()) {
            throw std::runtime_error("Ran out of private network IPv4 addresses!");
        }
        m_LastUsedClientAddress = nextClientAddress;
        return VpnSettings{nextClientAddress, m_Config.route, m_Config.mtu, m_Config.dnsServer, m_Config.secret};
    }

    void checkIdleClients(const std::chrono::steady_clock::time_point& now) {
        for (auto it = m_Clients.begin(); it != m_Clients.end(); ) {
            if (it->second->isIdle(now)) {
                m_ClientAddressMap.erase(it->second->getClientVpnAddress().toInt());
                it = m_Clients.erase(it);
            }
            else {
                ++it;
            }
        }
    }
};
