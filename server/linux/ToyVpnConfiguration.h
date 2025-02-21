#pragma once

#include "libs/pcapplusplus/include/pcapplusplus/IpAddress.h"
#include <optional>

struct ToyVpnConfiguration {
    std::string &tunInterfaceName;
    uint16_t port;
    pcpp::IPv4Network &privateNetwork;
    std::string publicNetworkInterface;
    pcpp::IPv4Network &route;
    uint16_t mtu;
    std::string secret;
    std::optional<std::string> saveFilePath;
    std::optional<pcpp::IPv4Address> dnsServer;
};
