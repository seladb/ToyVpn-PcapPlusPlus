#pragma once

#include "Log.h"
#include "PacketHandler.h"
#include "ServerSocketWrapper.h"
#include "TunInterfaceWrapper.h"
#include "VpnSettings.h"
#include "libs/pcapplusplus/include/pcapplusplus/IpAddress.h"
#include <chrono>
#include <netinet/in.h>

class ClientHandler {
  public:
    ClientHandler(const ServerSocketWrapper &serverSocket,
                  const sockaddr_in6 &clientExternalAddress,
                  const TunInterfaceWrapper &tunInterface,
                  const VpnSettings &vpnSettings,
                  std::optional<PacketHandler> &packetHandler)
        : m_ServerSocket(serverSocket),
          m_ClientExternalAddress(clientExternalAddress),
          m_TunInterface(tunInterface), m_VpnSettings(vpnSettings),
          m_PacketHandler(packetHandler) {}

    template <std::size_t BUFFER_SIZE>
    void handleDataFromClient(const std::array<uint8_t, BUFFER_SIZE> &buffer,
                              size_t dataSize) {
        m_LastMessageTimestamp = std::chrono::steady_clock::now();
        switch (m_State) {
        case State::START: {
            if (dataSize < 2 || buffer[0] != 0) {
                break;
            }

            std::string secret(buffer.begin() + 1, buffer.begin() + dataSize);
            if (secret != m_VpnSettings.secret) {
                TOYVPN_LOG_ERROR("Got the wrong secret: '" << secret << "'");
                m_State = State::ERROR;
                break;
            }

            auto params = m_VpnSettings.toParamString();
            TOYVPN_LOG_DEBUG("Sending params to client: '" << params << "'");
            std::vector<uint8_t> paramsMessage;
            paramsMessage.push_back(0);
            paramsMessage.insert(paramsMessage.end(), params.begin(),
                                 params.end());
            auto bytesSent =
                m_ServerSocket.send(paramsMessage, m_ClientExternalAddress);
            if (bytesSent == -1) {
                m_State = State::ERROR;
            } else {
                m_State = State::CONNECTED;

                std::array<uint8_t, 16> ipv6AddressBytes;
                std::copy(std::begin(m_ClientExternalAddress.sin6_addr.s6_addr),
                          std::end(m_ClientExternalAddress.sin6_addr.s6_addr),
                          ipv6AddressBytes.begin());

                TOYVPN_LOG_INFO(
                    "New client connected! External address: ("
                    << pcpp::IPv6Address(ipv6AddressBytes).toString() << ","
                    << m_ClientExternalAddress.sin6_port << ")"
                    << ", Internal address: " << m_VpnSettings.clientAddress);
            }
            break;
        }
        case State::CONNECTED: {
            // Got a control packet
            if (dataSize == 1 && buffer[0] == 0) {
                break;
            }

            if (dataSize == 11 && buffer[0] == 0) {
                std::string message(buffer.begin() + 1,
                                    buffer.begin() + dataSize);
                if (message == m_DisconnectMessage) {
                    m_State = State::DISCONNECTED;

                    if (m_PacketHandler.has_value()) {
                        m_PacketHandler->clientDisconnected(
                            m_VpnSettings.clientAddress);
                    }

                    TOYVPN_LOG_INFO(
                        "Client disconnected: " << m_VpnSettings.clientAddress);
                    break;
                }
            }

            m_TunInterface.send(buffer, dataSize);

            if (m_PacketHandler.has_value()) {
                m_PacketHandler->handlePacket(m_VpnSettings.clientAddress,
                                              buffer, dataSize);
            }

            break;
        }
        default: {
        }
        }
    }

    template <std::size_t BUFFER_SIZE>
    void handleDataFromTun(const std::array<uint8_t, BUFFER_SIZE> &buffer,
                           size_t dataSize) {
        m_ServerSocket.send(buffer, dataSize, m_ClientExternalAddress);

        if (m_PacketHandler.has_value()) {
            m_PacketHandler->handlePacket(m_VpnSettings.clientAddress, buffer,
                                          dataSize);
        }
    }

    void disconnect() {
        if (m_State != State::CONNECTED) {
            return;
        }

        std::vector<uint8_t> disconnectMessage;
        disconnectMessage.push_back(0);
        disconnectMessage.insert(disconnectMessage.end(),
                                 m_DisconnectMessage.begin(),
                                 m_DisconnectMessage.end());
        for (int i = 0; i < 3; i++) {
            m_ServerSocket.send(disconnectMessage, m_ClientExternalAddress);
        }

        m_State = State::DISCONNECTED;

        if (m_PacketHandler.has_value()) {
            m_PacketHandler->clientDisconnected(m_VpnSettings.clientAddress);
        }

        TOYVPN_LOG_INFO("Client disconnected: " << m_VpnSettings.clientAddress);
    }

    pcpp::IPv4Address getClientVpnAddress() const {
        return m_VpnSettings.clientAddress;
    }

    bool isIdle(const std::chrono::steady_clock::time_point &now) {
        return m_State == State::DISCONNECTED ||
               now - m_LastMessageTimestamp > m_ClientIdleTimeoutSec;
    }

  private:
    enum class State { START, CONNECTED, DISCONNECTED, ERROR };

    constexpr static std::string_view m_DisconnectMessage = "DISCONNECT";
    constexpr static std::chrono::duration m_ClientIdleTimeoutSec =
        std::chrono::seconds(60);

    const ServerSocketWrapper &m_ServerSocket;
    const TunInterfaceWrapper &m_TunInterface;
    std::optional<PacketHandler> &m_PacketHandler;
    int m_BufferSize;
    State m_State = State::START;
    sockaddr_in6 m_ClientExternalAddress;
    VpnSettings m_VpnSettings;
    std::chrono::steady_clock::time_point m_LastMessageTimestamp;
};
