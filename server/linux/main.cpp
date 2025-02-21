#include "Log.h"
#include "ToyVpnConfiguration.h"
#include "ToyVpnServer.h"
#include "libs/AixLog/aixlog.hpp"
#include "libs/argparse/argparse.hpp"
#include "libs/pcapplusplus/include/pcapplusplus/IpAddress.h"
#include "libs/pcapplusplus/include/pcapplusplus/PcapLiveDeviceList.h"
#include "libs/pcapplusplus/include/pcapplusplus/SystemUtils.h"
#include <filesystem>

int main(int argc, char *argv[]) {
    argparse::ArgumentParser program("ToyVpnServer");

    std::string tunInterfaceName;
    program.add_argument("-t, --tun")
        .help("the TUN interface name to create")
        .default_value("tun0")
        .store_into(tunInterfaceName);

    std::uint16_t port;
    program.add_argument("-p", "--port")
        .help("the port to listen to for incoming connections")
        .required()
        .store_into(port);

    pcpp::IPv4Network privateNetwork = std::string("10.0.0.0/24");
    program.add_argument("-e", "--private-network")
        .help("the private network to use for the VPN connection")
        .default_value("10.0.0.0/24")
        .action([&privateNetwork](const std::string &value) {
            privateNetwork = value;
        });

    std::string publicNetworkInterface;
    program.add_argument("-i", "--public-network-iface")
        .help("the public network interface to use to send VPN traffic to the "
              "internet")
        .required()
        .action([&publicNetworkInterface](const std::string value) {
            if (!pcpp::PcapLiveDeviceList::getInstance()
                     .getPcapLiveDeviceByName(value)) {
                throw std::invalid_argument(
                    value + " isn't a valid network interface on this device");
            }
            publicNetworkInterface = value;
        });

    std::string secret;
    program.add_argument("-s", "--secret")
        .help("the secret to verify the client")
        .required()
        .store_into(secret);

    pcpp::IPv4Network route = std::string("0.0.0.0/0");
    std::string routeStr;
    program.add_argument("-r", "--route")
        .help("the forwarding route")
        .default_value(route)
        .action([&route](const std::string &value) { route = value; });

    int mtu = 1400;
    program.add_argument("-m", "--mtu")
        .help("maximum transmission unit (MTU)")
        .default_value(1400)
        .action([&](const std::string &value) {
            try {
                mtu = std::stoi(value);
                if (mtu < 1000 || mtu > 1500) {
                    throw std::invalid_argument(
                        "MTU has to be between 1000 and 1500");
                }
            } catch (const std::invalid_argument &e) {
                throw std::invalid_argument("MTU is an invalid number");
            }
        });

    std::optional<pcpp::IPv4Address> dnsServer;
    program.add_argument("-d", "--dns-server")
        .help("DNS server to use")
        .action([&dnsServer](const std::string &value) { dnsServer = value; });

    std::optional<std::string> saveNetworkTrafficToFiles;
    program.add_argument("-f", "--save-to-files")
        .help("save all network traffic to pcapng files")
        .default_value("")
        .action([&saveNetworkTrafficToFiles](const std::string &value) {
            if (!std::filesystem::is_directory(value)) {
                throw std::invalid_argument(value + " is not a valid path");
            }
            std::string fileName =
                value + (!value.empty() && value.back() != '/' ? "/" : "");
            saveNetworkTrafficToFiles.emplace(fileName);
        });

    program.add_argument("-l", "--verbose")
        .help("print verbose log messages")
        .flag();

    try {
        program.parse_args(argc, argv);
    } catch (const std::exception &err) {
        std::cerr << err.what() << std::endl;
        std::cerr << program;
        return 1;
    }

    if (program.is_used("--save-to-files") &&
        !saveNetworkTrafficToFiles.has_value()) {
        saveNetworkTrafficToFiles.emplace("");
    }

    auto logLevel = program["--verbose"] == true ? AixLog::Severity::debug
                                                 : AixLog::Severity::info;
    AixLog::Log::init<AixLog::SinkCout>(logLevel,
                                        "%Y-%m-%d %H-%M-%S.#ms [#severity]");

    auto config = ToyVpnConfiguration{tunInterfaceName,
                                      port,
                                      privateNetwork,
                                      publicNetworkInterface,
                                      route,
                                      static_cast<uint16_t>(mtu),
                                      secret,
                                      saveNetworkTrafficToFiles,
                                      dnsServer};
    ToyVpnServer server(config);
    pcpp::ApplicationEventHandler::getInstance().onApplicationInterrupted(
        [](void *cookie) {
            auto server = reinterpret_cast<ToyVpnServer *>(cookie);
            server->stop();
        },
        &server);

    try {
        server.start();
    } catch (const std::exception &err) {
        TOYVPN_LOG_ERROR("An error occurred: " << err.what());
        return 1;
    }

    return 0;
}
