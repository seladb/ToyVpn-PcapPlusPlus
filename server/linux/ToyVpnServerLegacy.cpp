/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <fcntl.h>
#include <iomanip>

#ifdef __linux__

// There are several ways to play with this program. Here we just give an
// example for the simplest scenario. Let us say that a Linux box has a
// public IPv4 address on eth0. Please try the following steps and adjust
// the parameters when necessary.
//
// # Enable IP forwarding
// echo 1 > /proc/sys/net/ipv4/ip_forward
//
// # Pick a range of private addresses and perform NAT over eth0.
// iptables -t nat -A POSTROUTING -s 10.0.0.0/8 -o eth0 -j MASQUERADE
//
// # Create a TUN interface.
// ip tuntap add dev tun0 mode tun
//
// # Set the addresses and bring up the interface.
// ifconfig tun0 10.0.0.1 dstaddr 10.0.0.2 up
//
// # Create a server on port 8000 with shared secret "test".
// ./ToyVpnServer tun0 8000 test -m 1400 -a 10.0.0.2 32 -d 8.8.8.8 -r 0.0.0.0 0
//
// This program only handles a session at a time. To allow multiple sessions,
// multiple servers can be created on the same port, but each of them requires
// its own TUN interface. A short shell script will be sufficient. Since this
// program is designed for demonstration purpose, it performs neither strong
// authentication nor encryption. DO NOT USE IT IN PRODUCTION!

#include <net/if.h>
#include <linux/if_tun.h>
#include <iostream>
#include <cstring>
#include <arpa/inet.h>

// IP Header Structure (IPv4)
struct ip_header {
    unsigned char  iph_ihl:4, iph_ver:4;     // IP Header length and Version
    unsigned char  iph_tos;                  // Type of service
    unsigned short iph_len;                  // Total length
    unsigned short iph_id;                   // Identification
    unsigned short iph_offset;               // Fragment offset field
    unsigned char  iph_ttl;                  // Time to live
    unsigned char  iph_protocol;             // Protocol type
    unsigned short iph_checksum;             // IP checksum
    unsigned int   iph_saddr;                // Source address
    unsigned int   iph_daddr;                // Destination address
};

// IP Header size
#define IP_HEADER_SIZE 20

void print_hex(const char *data, size_t length) {
    std::cout << "Packet data (hex):\n";

    // Iterate over each byte in the packet
    for (size_t i = 0; i < length; ++i) {
        // Print each byte in hex format
        std::cout << std::setw(2) << std::setfill('0') << std::hex << (0xFF & data[i]) << " ";

        // Print a newline after every 16 bytes for better readability
        if ((i + 1) % 16 == 0) {
            std::cout << std::endl;
        }
    }

    std::cout << std::dec << std::endl;
}

void parse_packet(const char *packet) {
    struct ip_header *ip = (struct ip_header*) packet; // Cast the packet to the IP header structure

    // Extract the source and destination IP addresses
    struct in_addr src_addr, dest_addr;
    src_addr.s_addr = ip->iph_saddr;
    dest_addr.s_addr = ip->iph_daddr;

    // Convert IPs to readable format
    char src_ip[INET_ADDRSTRLEN], dest_ip[INET_ADDRSTRLEN];
    inet_ntop(AF_INET, &src_addr, src_ip, INET_ADDRSTRLEN);
    inet_ntop(AF_INET, &dest_addr, dest_ip, INET_ADDRSTRLEN);

    // Print basic information about the IP packet
    std::cout << "IP Header:\n";
    std::cout << "  Version: " << (unsigned int)ip->iph_ver << std::endl;
    std::cout << "  Header Length: " << (unsigned int)ip->iph_ihl * 4 << " bytes" << std::endl;
    std::cout << "  Total Length: " << ntohs(ip->iph_len) << " bytes" << std::endl;
    std::cout << "  Protocol: " << (unsigned int)ip->iph_protocol << std::endl;
    std::cout << "  Source IP: " << src_ip << std::endl;
    std::cout << "  Destination IP: " << dest_ip << std::endl;

    // Now parse the payload based on protocol (TCP/UDP)
    unsigned short protocol = ip->iph_protocol;

    // Parse based on protocol type
    if (protocol == 6) {  // TCP (Protocol 6)
        std::cout << "  Protocol: TCP\n";
        // You can parse the TCP header here
    } else if (protocol == 17) {  // UDP (Protocol 17)
        std::cout << "  Protocol: UDP\n";
        // You can parse the UDP header here
    } else {
        std::cout << "  Unknown Protocol\n";
    }
}


static int get_interface(char *name)
{
    int interface = open("/dev/net/tun", O_RDWR | O_NONBLOCK);

    ifreq ifr;
    memset(&ifr, 0, sizeof(ifr));
    ifr.ifr_flags = IFF_TUN | IFF_NO_PI;
    strncpy(ifr.ifr_name, name, sizeof(ifr.ifr_name));

    if (ioctl(interface, TUNSETIFF, &ifr)) {
        perror("Cannot get TUN interface");
        exit(1);
    }

    return interface;
}

#else

#error Sorry, you have to implement this part by yourself.

#endif

static int get_tunnel(char *port, char *secret)
{
    printf("DEBUG: starting get_tunnel\n");
    // We use an IPv6 socket to cover both IPv4 and IPv6.
    int tunnel = socket(AF_INET6, SOCK_DGRAM, 0);
    int flag = 1;
    setsockopt(tunnel, SOL_SOCKET, SO_REUSEADDR, &flag, sizeof(flag));
    flag = 0;
    setsockopt(tunnel, IPPROTO_IPV6, IPV6_V6ONLY, &flag, sizeof(flag));

    printf("DEBUG: get_tunnel - finish setting up the socket\n");
    printf("DEBUG: get_tunnel - port is %s, secret is: %s\n", port, secret);
    // Accept packets received on any local address.
    sockaddr_in6 addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin6_family = AF_INET6;
    addr.sin6_port = htons(atoi(port));

    // Call bind(2) in a loop since Linux does not have SO_REUSEPORT.
    while (bind(tunnel, (sockaddr *)&addr, sizeof(addr))) {
        if (errno != EADDRINUSE) {
            return -1;
        }
        usleep(100000);
    }

    printf("DEBUG: finish bind\n");

    // Receive packets till the secret matches.
    char packet[1024];
    socklen_t addrlen;
    do {
        addrlen = sizeof(addr);
        printf("DEBUG: before recvfrom\n");
        int n = recvfrom(tunnel, packet, sizeof(packet), 0,
                (sockaddr *)&addr, &addrlen);
        printf("DEBUG: after recvfrom\n");
        if (n <= 0) {
            return -1;
        }
        printf("DEBUG: receive data of size %d\n", n);
        packet[n] = 0;
    } while (packet[0] != 0 || strcmp(secret, &packet[1]));

    printf("DEBUG: got the secret!!\n");
    // Connect to the client as we only handle one client at a time.
    connect(tunnel, (sockaddr *)&addr, addrlen);
    return tunnel;
}

static void build_parameters(char *parameters, int size, int argc, char **argv)
{
    // Well, for simplicity, we just concatenate them (almost) blindly.
    int offset = 0;
    for (int i = 4; i < argc; ++i) {
        char *parameter = argv[i];
        int length = strlen(parameter);
        char delimiter = ',';

        // If it looks like an option, prepend a space instead of a comma.
        if (length == 2 && parameter[0] == '-') {
            ++parameter;
            --length;
            delimiter = ' ';
        }

        // This is just a demo app, really.
        if (offset + length >= size) {
            puts("Parameters are too large");
            exit(1);
        }

        // Append the delimiter and the parameter.
        parameters[offset] = delimiter;
        memcpy(&parameters[offset + 1], parameter, length);
        offset += 1 + length;
    }

    // Fill the rest of the space with spaces.
    memset(&parameters[offset], ' ', size - offset);

    // Control messages always start with zero.
    parameters[0] = 0;
}

//-----------------------------------------------------------------------------

int main(int argc, char **argv)
{
    if (argc < 5) {
        printf("Usage: %s <tunN> <port> <secret> options...\n"
               "\n"
               "Options:\n"
               "  -m <MTU> for the maximum transmission unit\n"
               "  -a <address> <prefix-length> for the private address\n"
               "  -r <address> <prefix-length> for the forwarding route\n"
               "  -d <address> for the domain name server\n"
               "  -s <domain> for the search domain\n"
               "\n"
               "Note that TUN interface needs to be configured properly\n"
               "BEFORE running this program. For more information, please\n"
               "read the comments in the source code.\n\n", argv[0]);
        exit(1);
    }

    printf("DEBUG: main start!!!\n");
    // Parse the arguments and set the parameters.
    char parameters[1024];
    build_parameters(parameters, sizeof(parameters), argc, argv);

    // Get TUN interface.
    int interface = get_interface(argv[1]);

    printf("DEBUG: interface = %d\n", interface);

    // Wait for a tunnel.
    int tunnel;
    while ((tunnel = get_tunnel(argv[2], argv[3])) != -1) {
        printf("%s: Here comes a new tunnel\n", argv[1]);

        // On UN*X, there are many ways to deal with multiple file
        // descriptors, such as poll(2), select(2), epoll(7) on Linux,
        // kqueue(2) on FreeBSD, pthread(3), or even fork(2). Here we
        // mimic everything from the client, so their source code can
        // be easily compared side by side.

        // Put the tunnel into non-blocking mode.
        fcntl(tunnel, F_SETFL, O_NONBLOCK);

        // Send the parameters several times in case of packet loss.
        for (int i = 0; i < 3; ++i) {
            send(tunnel, parameters, sizeof(parameters), MSG_NOSIGNAL);
        }

        // Allocate the buffer for a single packet.
        char packet[32767];

        // We use a timer to determine the status of the tunnel. It
        // works on both sides. A positive value means sending, and
        // any other means receiving. We start with receiving.
        int timer = 0;

        // We keep forwarding packets till something goes wrong.
        while (true) {
            // Assume that we did not make any progress in this iteration.
            bool idle = true;

            // Read the outgoing packet from the input stream.
            int length = read(interface, packet, sizeof(packet));
            if (length > 0) {
                printf("DEBUG: read a packet from interface of length %d\n", length);
                print_hex(packet, length);
                parse_packet(packet);

                // Write the outgoing packet to the tunnel.
                send(tunnel, packet, length, MSG_NOSIGNAL);

                // There might be more outgoing packets.
                idle = false;

                // If we were receiving, switch to sending.
                if (timer < 1) {
                    timer = 1;
                }
            }

            // Read the incoming packet from the tunnel.
            length = recv(tunnel, packet, sizeof(packet), 0);
            if (length == 0) {
                break;
            }
            if (length > 0) {
                // Ignore control messages, which start with zero.
                if (packet[0] != 0) {
                    printf("DEBUG: read a packet from tunnel of length %d\n", length);
                    // Write the incoming packet to the output stream.
                    write(interface, packet, length);
                }

                // There might be more incoming packets.
                idle = false;

                // If we were sending, switch to receiving.
                if (timer > 0) {
                    timer = 0;
                }
            }

            // If we are idle or waiting for the network, sleep for a
            // fraction of time to avoid busy looping.
            if (idle) {
                usleep(100000);

                // Increase the timer. This is inaccurate but good enough,
                // since everything is operated in non-blocking mode.
                timer += (timer > 0) ? 100 : -100;

                // We are receiving for a long time but not sending.
                // Can you figure out why we use a different value? :)
                if (timer < -16000) {
                    // Send empty control messages.
                    packet[0] = 0;
                    for (int i = 0; i < 3; ++i) {
                        send(tunnel, packet, 1, MSG_NOSIGNAL);
                    }

                    // Switch to sending.
                    timer = 1;
                }

                // We are sending for a long time but not receiving.
                if (timer > 20000) {
                    break;
                }
            }
        }
        printf("%s: The tunnel is broken\n", argv[1]);
        close(tunnel);
    }
    perror("Cannot create tunnels");
    exit(1);
}
