#include <string>
#include <sstream>
#include <jni.h>
#include <android/log.h>
#include "../../../libs/pcapplusplus/include/PacketUtils.h"
#include "../../../libs/pcapplusplus/include/Packet.h"
#include "../../../libs/pcapplusplus/include/DnsLayer.h"
#include "../../../libs/pcapplusplus/include/SSLLayer.h"
#include "../../../libs/pcapplusplus/include/PcapFileDevice.h"

const char* TAG = "PcapPlusPlusNativeInterface";

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

class PcapFileSingleton {
public:
    bool init(std::string filePath) {
        if (m_PcapNgFile != NULL) {
            return true;
        }
        m_PcapNgFile = new pcpp::PcapNgFileWriterDevice(filePath);
        return m_PcapNgFile->open();
    }
    bool isOpen() {
        return m_PcapNgFile != NULL;
    }
    pcpp::PcapNgFileWriterDevice* getPcapFileDevice() {
        return m_PcapNgFile;
    }
    void close() {
        if (m_PcapNgFile == NULL) {
            return;
        }
        LOGI("Closing pcap file: %s", m_PcapNgFile->getFileName().c_str());
        m_PcapNgFile->close();
        delete m_PcapNgFile;
        m_PcapNgFile = NULL;

    }

    static PcapFileSingleton& getInstance()
    {
        static PcapFileSingleton instance;
        return instance;
    }
private:
    // private default c'tor and d'tor because this object is a singleton
    PcapFileSingleton() : m_PcapNgFile(NULL) {}
    ~PcapFileSingleton() { close(); }

    pcpp::PcapNgFileWriterDevice* m_PcapNgFile;

};


extern "C"
JNIEXPORT void JNICALL
Java_com_example_android_pcapplusplus_PcapPlusPlusInterface_openPcapFileNative(JNIEnv *env, jobject thiz, jstring filesDir) {
    jboolean isCopy;
    const char* filesDirAsCharArray = (env)->GetStringUTFChars(filesDir, &isCopy);
    std::string filesDirString = std::string(filesDirAsCharArray, strlen(filesDirAsCharArray));
    std::string pcapFilePath = filesDirString + "/toy_vpn.pcap";
    LOGI("Opening pcap file at: %s", pcapFilePath.c_str());
    if (!PcapFileSingleton::getInstance().init(pcapFilePath)) {
        LOGE("Error opening pcap file!");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_android_pcapplusplus_PcapPlusPlusInterface_closePcapFileNative(JNIEnv *env, jobject thiz) {
    PcapFileSingleton::getInstance().close();
    LOGI("Pcap file closed");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_android_pcapplusplus_PcapPlusPlusInterface_analyzePacketNative(JNIEnv *env, jobject thiz, jbyteArray packet, jint packetLength) {
    jboolean isCopy;
    jbyte* packetBytes = env->GetByteArrayElements(packet, &isCopy);
    std::stringstream sstream;
    sstream << "{";
    if (packetBytes == NULL) {
        LOGE("packet is NULL!");
    }
    else {
        timeval time;
        gettimeofday(&time, NULL);
        pcpp::RawPacket rawPacket((const uint8_t*)packetBytes, packetLength, time, false, pcpp::LINKTYPE_RAW);
        if (PcapFileSingleton::getInstance().isOpen()) {
            PcapFileSingleton::getInstance().getPcapFileDevice()->writePacket(rawPacket);
        }
        pcpp::Packet parsedPacket(&rawPacket);
        uint32_t connectionID = pcpp::hash5Tuple(&parsedPacket);
        sstream << "'connectionID':" << connectionID;
        if (parsedPacket.isPacketOfType(pcpp::IPv4)) {
            sstream << ",'ipv4': 1";
        }
        if (parsedPacket.isPacketOfType(pcpp::IPv6)) {
            sstream << ",'ipv6': 1";
        }
        if (parsedPacket.isPacketOfType(pcpp::TCP)) {
            sstream << ",'tcp': 1";
        }
        if (parsedPacket.isPacketOfType(pcpp::UDP)) {
            sstream << ",'udp': 1";
        }
        if (parsedPacket.isPacketOfType(pcpp::DNS)) {
            pcpp::DnsLayer* dnsLayer = parsedPacket.getLayerOfType<pcpp::DnsLayer>();
            if (dnsLayer->getDnsHeader()->queryOrResponse == 0)
                sstream << ",'dns':'dnsRequest'";
            else
                sstream << ",'dns':'dnsResponse'";
        }
        if (parsedPacket.isPacketOfType(pcpp::SSL)) {
            pcpp::SSLHandshakeLayer* sslHandshakeLayer = parsedPacket.getLayerOfType<pcpp::SSLHandshakeLayer>();
            if (sslHandshakeLayer != NULL) {
                sstream << ",'tls':{";
                pcpp::SSLClientHelloMessage* clientHelloMessage = sslHandshakeLayer->getHandshakeMessageOfType<pcpp::SSLClientHelloMessage>();
                if (clientHelloMessage != NULL) {
                    pcpp::SSLServerNameIndicationExtension *sniExt = clientHelloMessage->getExtensionOfType<pcpp::SSLServerNameIndicationExtension>();
                    if (sniExt != NULL) {
                        sstream << "'sni':'" << sniExt->getHostName() << "'";
                    }
                }
                pcpp::SSLServerHelloMessage* serverHelloMessage = sslHandshakeLayer->getHandshakeMessageOfType<pcpp::SSLServerHelloMessage>();
                if (serverHelloMessage != NULL) {
                    sstream << "'version':'" << serverHelloMessage->getHandshakeVersion().toString() << "'";
                }
                sstream << "}";
            }
        }
    }
    sstream << "}";
    std::string response = sstream.str();
    jstring responseAsJstring = env->NewStringUTF(response.c_str());
    return responseAsJstring;
}