#pragma once

#include <arpa/inet.h>
#include <cstring>
#include <functional>
#include <netinet/in.h>

struct sockaddrIn6Hash {
    std::size_t operator()(const sockaddr_in6 &sa) const {
        std::size_t h1 = std::hash<uint16_t>{}(sa.sin6_port);
        std::size_t h2 = std::hash<uint16_t>{}(sa.sin6_family);

        // Use the address bytes for a more unique hash
        std::size_t h3 = 0;
        for (int i = 0; i < 16; ++i) {
            h3 ^= std::hash<uint8_t>{}(sa.sin6_addr.s6_addr[i])
                  << (i % (8 * sizeof(std::size_t)));
        }

        return h1 ^ h2 ^ h3;
    }
};

// Equality function for sockaddr_in6 (for comparison in unordered_map)
struct sockaddrIn6Equal {
    bool operator()(const sockaddr_in6 &sa1, const sockaddr_in6 &sa2) const {
        return sa1.sin6_port == sa2.sin6_port &&
               sa1.sin6_family == sa2.sin6_family &&
               std::memcmp(&sa1.sin6_addr, &sa2.sin6_addr,
                           sizeof(sa1.sin6_addr)) == 0;
    }
};
