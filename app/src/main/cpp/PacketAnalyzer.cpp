#include <DnsLayer.h>
#include <Packet.h>
#include <PacketUtils.h>
#include <RawPacket.h>
#include <SSLLayer.h>
#include <android/log.h>
#include <iomanip>
#include <iostream>
#include <jni.h>
#include <sstream>
#include <string>
#include <vector>

//#define LOG_TAG "ToyVpnNativeCode"
//
//#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

void analyzeDnsPacket(const pcpp::Packet &packet,
                      std::ostringstream &jsonStream) {
    auto dnsLayer = packet.getLayerOfType<pcpp::DnsLayer>();

    if (dnsLayer == nullptr) {
        return;
    }

    if (dnsLayer->getDnsHeader()->queryOrResponse == 0 &&
        dnsLayer->getQueryCount() > 0) {
        auto dnsQuery = dnsLayer->getFirstQuery();
        jsonStream << R"("dnsQuery":")" << dnsQuery->getName() << "\",";
    }
}

void analyzeTlsPacket(const pcpp::Packet &packet,
                      std::ostringstream &jsonStream) {
    auto tlsLayer = packet.getLayerOfType<pcpp::SSLLayer>();

    if (tlsLayer == nullptr) {
        return;
    }

    if (tlsLayer->getRecordType() != pcpp::SSL_HANDSHAKE) {
        return;
    }

    auto handshakeLayer = dynamic_cast<pcpp::SSLHandshakeLayer *>(tlsLayer);
    if (handshakeLayer == nullptr) {
        return;
    }

    auto clientHelloMessage =
        handshakeLayer
            ->getHandshakeMessageOfType<pcpp::SSLClientHelloMessage>();

    if (clientHelloMessage != nullptr) {
        auto sniExt =
            clientHelloMessage
                ->getExtensionOfType<pcpp::SSLServerNameIndicationExtension>();
        if (sniExt != nullptr) {
            jsonStream << R"("tlsServerName":")" << sniExt->getHostName()
                       << "\",";
        }
    }

    auto serverHelloMessage =
        handshakeLayer
            ->getHandshakeMessageOfType<pcpp::SSLServerHelloMessage>();

    if (serverHelloMessage != nullptr) {
        jsonStream << "\"tlsVersion\":"
                   << serverHelloMessage->getHandshakeVersion().asUInt() << ",";
    }
}

extern "C" {
JNIEXPORT jbyteArray JNICALL
Java_com_pcapplusplus_toyvpn_pcapplusplus_PcapPlusPlusInterface_analyzePacketNative(
    JNIEnv *env, jobject obj, jbyteArray packetData) {
    jsize length = env->GetArrayLength(packetData);
    jbyte *rawPacketData = env->GetByteArrayElements(packetData, nullptr);

    std::vector<uint8_t> packetVector(rawPacketData, rawPacketData + length);

    auto now = std::chrono::system_clock::now();
    auto duration = now.time_since_epoch();
    auto seconds = std::chrono::duration_cast<std::chrono::seconds>(duration);
    auto nanoseconds =
        std::chrono::duration_cast<std::chrono::nanoseconds>(duration) -
        seconds;
    struct timespec ts{};
    ts.tv_sec = seconds.count();
    ts.tv_nsec = nanoseconds.count();

    pcpp::RawPacket rawPacket(packetVector.data(), packetVector.size(), ts,
                              false, pcpp::LINKTYPE_DLT_RAW1);

    pcpp::Packet packet(&rawPacket);

    std::ostringstream jsonStream;
    jsonStream << "{";

    auto isIPv4 = packet.isPacketOfType(pcpp::IPv4);
    auto isIPv6 = packet.isPacketOfType(pcpp::IPv6);
    auto isTCP = packet.isPacketOfType(pcpp::TCP);
    auto isUDP = packet.isPacketOfType(pcpp::UDP);
    auto isDNS = packet.isPacketOfType(pcpp::DNS);
    auto isTLS = packet.isPacketOfType(pcpp::SSL);

    if (isIPv4) {
        jsonStream << "\"isIPv4\":true,";
    }

    if (isIPv6) {
        jsonStream << "\"isIPv6\":true,";
    }

    if (isTCP) {
        jsonStream << "\"isTCP\":true,";
    }

    if (isUDP) {
        jsonStream << "\"isUDP\":true,";
    }

    if (isTCP || isUDP) {
        jsonStream << "\"connectionID\":" << pcpp::hash5Tuple(&packet) << ",";
    }

    if (isDNS) {
        jsonStream << "\"isDNS\":true,";
        analyzeDnsPacket(packet, jsonStream);
    }

    if (isTLS) {
        jsonStream << "\"isTLS\":true,";
        analyzeTlsPacket(packet, jsonStream);
    }

    jsonStream << "\"length\":" << length << "}";

    auto jsonString = jsonStream.str();

    jbyteArray result = env->NewByteArray(jsonString.length());

    env->SetByteArrayRegion(
        result, 0, jsonString.length(),
        reinterpret_cast<const jbyte *>(jsonString.c_str()));

    env->ReleaseByteArrayElements(packetData, rawPacketData, 0);

    return result;
}
}