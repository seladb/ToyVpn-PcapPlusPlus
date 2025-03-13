# ToyVPN Server 🌐

## Overview 🛠️
This is the VPN server implemented in C++. It utilizes various networking libraries to handle packet forwarding, routing, and tunneling. The server is designed to manage VPN connections, forward traffic, and provide essential network functionalities.

It is heavily inspired by Android ToyVpn and provides a robust solution for handling VPN connections.

## Features 🚀
- Supports multiple clients
- Single-threaded, utilizing epoll for efficient event handling
- Automatic configuration: creates tun interface, configures iptables and IP routing, and handles automatic teardown
- Records pcapng files with traffic per VPN client

## Dependencies 🔗
This project relies on the following libraries:
- [PcapPlusPlus](https://github.com/seladb/PcapPlusPlus) for packet parsing and saving packets to pcapng files
- [concurrentqueue](https://github.com/cameron314/concurrentqueue) for passing packets to save without interfering with the main processing thread
- [argparse](https://github.com/p-ranav/argparse) for parsing CLI arguments
- [AixLog](https://github.com/berkus/AixLog) for logging

## Building the Project 🛠️
### Prerequisites ✅
Ensure you have the following installed:
- A C++ compiler supporting C++17
- CMake (version 3.20 or higher)
- `libpcap-dev` (required for `pcapplusplus`)

### Installing PcapPlusPlus 📦
- Go to the latest release page: https://github.com/seladb/PcapPlusPlus/releases
- Download the relevant binary for your operating system
- Extract the archive file under `libs/`
- Rename the folder to `pcapplusplus`

### Build Instructions 🏗️
```sh
mkdir build && cd build
cmake ..
make
```

## Running the Server 🚀
```sh
sudo ./ToyVpnServer --port 5678 --public-network-iface eth0 --secret my_secret
```

## How to Use ⚙️
The server supports various CLI flags for configuration:

```sh
Usage: ToyVpnServer [--help] [--version] [-t, --tun VAR] --port VAR [--private-network VAR] --public-network-iface VAR --secret VAR [--route VAR] [--mtu VAR] [--dns-server VAR] [--save-to-files VAR] [--verbose]

Optional arguments:
  -h, --help                  shows help message and exits
  -v, --version               prints version information and exits
  -t, --tun                   the TUN interface name to create [nargs=0..1] [default: "tun0"]
  -p, --port                  the port to listen to for incoming connections [required]
  -e, --private-network       the private network to use for the VPN connection [nargs=0..1] [default: "10.0.0.0/24"]
  -i, --public-network-iface  the public network interface to use to send VPN traffic to the internet [required]
  -s, --secret                the secret to verify the client [required]
  -r, --route                 the forwarding route [nargs=0..1] [default: 0.0.0.0/0]
  -m, --mtu                   maximum transmission unit (MTU) [nargs=0..1] [default: 1400]
  -d, --dns-server            DNS server to use
  -f, --save-to-files         save all network traffic to pcapng files [nargs=0..1] [default: ""]
  -l, --verbose               print verbose log messages
```

## Architecture 🏛️
### Platform Support 🐧
This server is designed to work exclusively on Linux.

### Main Components 🔩
- `EpollWrapper.h`: Manages event-driven networking
- `ServerSocketWrapper.h`: Manages the UDP socket that listens to incoming clients
- `ClientHandler.h`: Handles the lifecycle of individual VPN clients - connecting to a new client, performing the handshake protocol, managing traffic from the client to the external network and vice versa, handling client disconnection
- `TunInterfaceWrapper.h`: Creates and manages the TUN interface used for the VPN connection
- `NatAndRoutingWrapper.h`: Configures IP forwarding and routing. Uses `iptables` and `ip route`
- `PacketHandler.h`: Run in a separate thread, accepts packets from the main thread and saves them to pcapng files per VPN client
- `ToyVpnServer.h`: Orchestrates all of the above and manages the server lifecycle

### Flow 🔄
1. The server initializes and sets up a TUN interface.
2. It configures IP forwarding and routing.
3. Opens a UDP socket and starts listening to incoming clients.
4. Clients connect, go through the handshake protocol and establish a VPN session.
5. Packets are forwarded between clients and the external network (Internet).
6. If requested by the user, packets are also passed to a different thread for saving them to pcapng files.
7. When a client disconnects, the server cleans up the resources.
8. When the server shuts down it removes the TUN interface and restores the IP forwarding and routing configurations.

### Handshake Protocol 🤝
The handshake process establishes a connection between the client and the server, exchanging configuration details and authentication data:
1. The client connects to the UDP socket the server listens on
2. It sends the secret to the server and the server verifies the secret
3. The server allocates a new internal IP address for the client and send configuration parameters to the client that include:
    - Client internal IP address
    - The forwarding route (`0.0.0.0/0` by default)
    - MTU
    - DNS server to use if configured by the server
4. The client sends control packets once in a while to indicate it's still connected
5. When a client wants to disconnect, it sends a special control message
6. If the server shuts down, it sends a special disconnect control message to all connected clients

## License 📜
This project is licensed under the MIT License.

