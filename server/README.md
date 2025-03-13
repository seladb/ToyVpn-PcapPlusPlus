# ToyVPN Server 🌐

## Overview 🛠️
ToyVPN is a lightweight VPN server implemented in C++. It leverages **PcapPlusPlus** and various networking tools to handle packet forwarding, routing, and tunneling. The server is designed to efficiently manage VPN connections, forward traffic, and provide essential network functionalities.

Inspired by [Android ToyVpn](https://android.googlesource.com/platform/development/+/master/samples/ToyVpn), this project offers a robust solution for handling VPN connections.

## Features 🚀
- **Multi-client support**: Handles multiple VPN clients simultaneously.
- **Efficient event handling**: Uses **epoll** for scalable performance.
- **Automatic setup & teardown**:
    - Creates a TUN interface dynamically.
    - Configures **iptables** and **IP routing**.
    - Cleans up configurations upon shutdown.
- **Traffic logging**: Saves network traffic per client as **pcapng** files.

## Dependencies 🔗
This project relies on the following libraries:
- **[PcapPlusPlus](https://github.com/seladb/PcapPlusPlus)** - Packet parsing & pcapng file generation.
- **[concurrentqueue](https://github.com/cameron314/concurrentqueue)** - Non-blocking queue for efficient packet handling.
- **[argparse](https://github.com/p-ranav/argparse)** - CLI argument parsing.
- **[AixLog](https://github.com/berkus/AixLog)** - Logging framework.

## Building the Project 🏗️
### Prerequisites ✅
Ensure you have the following installed:
- A **C++17** compatible compiler.
- **CMake** (version **3.20+**).
- **libpcap-dev** (required for `pcapplusplus`).

### Installing PcapPlusPlus 📦
1. Download the latest release from: [PcapPlusPlus Releases](https://github.com/seladb/PcapPlusPlus/releases).
2. Extract the archive into the `libs/` directory.
3. Rename the extracted folder to `pcapplusplus`.

### Build Instructions ⚒️
```sh
mkdir build && cd build
cmake ..
make
```

## Running the Server 🚀
### Basic Usage
The following command starts the VPN server:
```sh
sudo ./ToyVpnServer --port 5678 --public-network-iface eth0 --secret my_secret
```
- **Port:** `5678` (for incoming VPN connections).
- **Public Network Interface:** `eth0` (provides Internet access).
- **Secret Key:** `my_secret` (used for authentication).

### CLI Options ⚙️
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
This server is **Linux-only** due to its reliance on platform-specific networking tools.

### Main Components 🔩
- **`EpollWrapper.h`** - Manages event-driven networking.
- **`ServerSocketWrapper.h`** - Handles the UDP socket for client connections.
- **`ClientHandler.h`** - Manages VPN client sessions, including:
    - Connection establishment.
    - Handshake protocol.
    - Traffic forwarding.
    - Disconnection handling.
- **`TunInterfaceWrapper.h`** - Manages the TUN interface for VPN traffic.
- **`NatAndRoutingWrapper.h`** - Configures NAT and routing using `iptables`.
- **`PacketHandler.h`** - Runs in a separate thread to log VPN traffic.
- **`ToyVpnServer.h`** - Orchestrates all components and manages the server lifecycle.

### Server Flow 🔄
1. Initializes and configures a **TUN interface**.
2. Sets up **IP forwarding and routing**.
3. Opens a **UDP socket** to listen for clients.
4. Handles client **authentication** and **session establishment**.
5. Forwards packets between clients and the external network.
6. Saves network traffic logs (if enabled).
7. Cleans up resources upon client disconnection or server shutdown.

### Handshake Protocol 🤝
1. The client connects to the UDP port.
2. It sends the **secret key** for authentication.
3. If valid, the server:
    - Assigns an **internal IP** to the client.
    - Sends the **routing parameters**.
    - Provides **DNS settings** (if configured).
    - Sets **MTU**.
4. The client sends **keep-alive** packets periodically.
5. Disconnection occurs via:
    - Explicit **client request**.
    - **Server shutdown** (broadcasts a disconnect message).

## License 📜
This project is licensed under the **MIT License**.

