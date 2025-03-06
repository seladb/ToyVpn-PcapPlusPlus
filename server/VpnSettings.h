#pragma once

#include "libs/pcapplusplus/include/pcapplusplus/IpAddress.h"
#include <optional>

struct VpnSettings {
    pcpp::IPv4Address clientAddress;
    pcpp::IPv4Network routeAddress;
    uint16_t mtu;
    std::optional<pcpp::IPv4Address> dnsServer;
    std::string secret;

    std::string toParamString() const {
        std::ostringstream params;
        params << "a," << clientAddress.toString() << ",32 "
               << "r," << routeAddress.getNetworkPrefix().toString() << ","
               << static_cast<int>(routeAddress.getPrefixLen()) << " "
               << "m," << mtu;

        if (dnsServer) {
            params << " d," << dnsServer.value().toString();
        }

        return params.str();
    }
};
