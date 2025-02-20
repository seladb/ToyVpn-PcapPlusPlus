#pragma once

#include <iostream>
#include <net/if.h>
#include <fcntl.h>
#include <linux/if_tun.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include "libs/pcapplusplus/include/pcapplusplus/IpAddress.h"
#include "Log.h"

class TunInterfaceWrapper {
public:
    void init(const std::string& tunInterfaceName, const pcpp::IPv4Network& privateNetwork)
    {
        int interface = open("/dev/net/tun", O_RDWR | O_NONBLOCK);

        ifreq ifr;
        memset(&ifr, 0, sizeof(ifr));
        ifr.ifr_flags = IFF_TUN | IFF_NO_PI;
        strncpy(ifr.ifr_name, tunInterfaceName.c_str(), sizeof(ifr.ifr_name));

        if (ioctl(interface, TUNSETIFF, &ifr)) {
            std::array<char, 256> buffer;
            throw std::runtime_error("Couldn't create TUN interface, ioctl() failed: " + std::string(strerror_r(errno, buffer.data(), buffer.size())));
        }

        std::ostringstream ifconfigCommand;
        ifconfigCommand
                << "ifconfig "
                << tunInterfaceName
                << " "
                << privateNetwork.getLowestAddress().toString()
                << " up";

        int result = std::system(ifconfigCommand.str().c_str());
        if (result != 0) {
            throw std::runtime_error("Couldn't set up TUN interface: '" + ifconfigCommand.str() + "'");
        }

        m_TunInterfaceName = tunInterfaceName;
        m_Interface = interface;
        m_TunIpAddress = privateNetwork.getLowestAddress();
        m_PrivateNetwork = privateNetwork;
        m_IsInitialized = true;

        TOYVPN_LOG_INFO("Created TUN interface '" << tunInterfaceName << "'");
    }

    int getInterfaceFd() const { return m_Interface; }

    const pcpp::IPv4Address& getTunIpAddress() const { return m_TunIpAddress; }

    template<std::size_t BUFFER_SIZE>
    size_t receive(std::array<uint8_t, BUFFER_SIZE>& buffer) const {
        if (!m_IsInitialized) {
            throw std::runtime_error("TUN interface is not initialized");
        }

        return read(m_Interface, buffer.data(), buffer.size());
    }

    template<std::size_t BUFFER_SIZE>
    size_t send(const std::array<uint8_t, BUFFER_SIZE>& buffer, size_t dataSize) const {
        if (!m_IsInitialized) {
            throw std::runtime_error("TUN interface is not initialized");
        }

        return write(m_Interface, buffer.data(), dataSize);
    }

private:
    std::string m_TunInterfaceName;
    pcpp::IPv4Network m_PrivateNetwork = std::string("0.0.0.0/0");
    pcpp::IPv4Address m_TunIpAddress;
    int m_Interface = 0;
    bool m_IsInitialized = false;
};
