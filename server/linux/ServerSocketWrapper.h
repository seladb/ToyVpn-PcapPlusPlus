#pragma once

#include "Log.h"
#include <iostream>
#include <netinet/in.h>
#include <unistd.h>
#include <vector>

class ServerSocketWrapper {
  public:
    virtual ~ServerSocketWrapper() {
        if (m_IsInitialized) {
            close(m_ServerSocket);
        }
    }

    void init(uint16_t port) {
        int serverSocket = socket(AF_INET6, SOCK_DGRAM, 0);
        if (serverSocket < 0) {
            throw std::runtime_error("Error creating server socket!");
        }

        int flag = 1;
        setsockopt(serverSocket, SOL_SOCKET, SO_REUSEADDR, &flag, sizeof(flag));

        // Dual stack - accept both IPv4 and IPv6 clients
        flag = 0;
        setsockopt(serverSocket, IPPROTO_IPV6, IPV6_V6ONLY, &flag,
                   sizeof(flag));

        sockaddr_in6 serverAddress;
        memset(&serverAddress, 0, sizeof(serverAddress));
        serverAddress.sin6_family = AF_INET6;
        serverAddress.sin6_port = htons(port);

        if (bind(serverSocket,
                 reinterpret_cast<struct sockaddr *>(&serverAddress),
                 sizeof(serverAddress)) < 0) {
            close(serverSocket);
            std::array<char, 256> buffer;
            throw std::runtime_error(
                "Error binding server socket: " +
                std::string(strerror_r(errno, buffer.data(), buffer.size())));
        }

        TOYVPN_LOG_INFO("Listening on UDP port " << port);
        m_ServerSocket = serverSocket;
        m_IsInitialized = true;
    }

    int getSocketFd() const { return m_ServerSocket; }

    template <std::size_t BUFFER_SIZE>
    size_t receive(std::array<uint8_t, BUFFER_SIZE> &buffer,
                   sockaddr_in6 &clientAddress) const {
        auto clientAddressLen = static_cast<socklen_t>(sizeof(clientAddress));
        return recvfrom(m_ServerSocket, buffer.data(), buffer.size(), 0,
                        reinterpret_cast<sockaddr *>(&clientAddress),
                        &clientAddressLen);
    }

    template <std::size_t BUFFER_SIZE>
    int send(const std::array<uint8_t, BUFFER_SIZE> &buffer, size_t dataSize,
             const sockaddr_in6 &sendTo) const {
        if (!m_IsInitialized) {
            throw std::runtime_error("TUN interface is not initialized");
        }

        return sendto(m_ServerSocket, buffer.data(), dataSize, 0,
                      reinterpret_cast<const sockaddr *>(&sendTo),
                      sizeof(sendTo));
    }

    int send(const std::vector<uint8_t> &buffer,
             const sockaddr_in6 &sendTo) const {
        if (!m_IsInitialized) {
            throw std::runtime_error("TUN interface is not initialized");
        }

        return sendto(m_ServerSocket, buffer.data(), buffer.size(), 0,
                      reinterpret_cast<const sockaddr *>(&sendTo),
                      sizeof(sendTo));
    }

  private:
    int m_ServerSocket = -1;
    bool m_IsInitialized = false;
};