#pragma once

#include <iostream>
#include <thread>
#include <atomic>
#include <chrono>
#include <unordered_map>
#include "libs/concurrentqueue/concurrentqueue.h"
#include "libs/pcapplusplus/include/pcapplusplus/IpAddress.h"
#include "libs/pcapplusplus/include/pcapplusplus/PcapFileDevice.h"
#include "Log.h"

class PacketHandler {
public:
    PacketHandler(const std::string& filePath) : m_FilePath(filePath) {
        m_Thread = std::thread(&PacketHandler::run, this);
    }

    virtual ~PacketHandler() {
        if (m_Thread.joinable()) {
            m_Thread.join();
        }
    }

    void stop() {
        m_StopFlag = true;
        if (m_Thread.joinable()) {
            m_Thread.join();
        }
    }

    void clientDisconnected(const pcpp::IPv4Address& clientAddress) {
        auto clientAddressValue = clientAddress.toInt();
        if (m_PcapWriters.find(clientAddressValue) != m_PcapWriters.end()) {
            m_PacketQueue.enqueue({clientAddress, {}});
        }
    }

    template<std::size_t BUFFER_SIZE>
    void handlePacket(const pcpp::IPv4Address& clientAddress, const std::array<uint8_t, BUFFER_SIZE>& buffer, size_t dataSize) {
        std::vector<uint8_t> bufferVector(buffer.begin(), buffer.begin() + dataSize);
        m_PacketQueue.enqueue({clientAddress, bufferVector});
    }

private:
    using PacketQueueItem = std::pair<pcpp::IPv4Address, std::vector<uint8_t>>;
    using PacketQueue = moodycamel::ConcurrentQueue<PacketQueueItem>;

    static constexpr size_t m_InitialQueueSize = 1000;
    static constexpr int m_DequeueBulkSize = 100;

    std::string m_FilePath;
    PacketQueue m_PacketQueue{m_InitialQueueSize};
    std::atomic<bool> m_StopFlag{false};
    std::thread m_Thread;
    std::unordered_map<uint32_t, std::unique_ptr<pcpp::PcapNgFileWriterDevice>> m_PcapWriters;

    void run() {
        TOYVPN_LOG_DEBUG("Starting packet handling thread");
        while (!m_StopFlag) {
            PacketQueueItem items[m_DequeueBulkSize];
            auto itemCount = m_PacketQueue.try_dequeue_bulk(items, m_DequeueBulkSize);
            auto timestamp = getCurrentTimestamp();
            for (auto it = std::begin(items); it != std::begin(items) + itemCount; ++it) {
                if (it->second.empty()) {
                    removePcapWriter(it->first);
                    continue;
                }
                auto pcapWriter = getOrCreatePcapWriter(it->first);
                pcpp::RawPacket rawPacket(it->second.data(), it->second.size(), timestamp, false, pcpp::LINKTYPE_DLT_RAW1);
                pcapWriter->writePacket(rawPacket);
            }
            std::this_thread::sleep_for(std::chrono::milliseconds (100));
        }
        TOYVPN_LOG_DEBUG("Stopping packet handling thread");
    }

    pcpp::PcapNgFileWriterDevice* getOrCreatePcapWriter(const pcpp::IPv4Address& clientAddress) {
        auto ipAddressValue = clientAddress.toInt();
        if (m_PcapWriters.find(ipAddressValue) == m_PcapWriters.end()) {
            auto fileName = clientAddress.toString();
            std::replace(fileName.begin(), fileName.end(), '.', '-');
            fileName.append(".pcapng");
            fileName.insert(0, m_FilePath);
            m_PcapWriters[ipAddressValue] = std::unique_ptr<pcpp::PcapNgFileWriterDevice>(new pcpp::PcapNgFileWriterDevice(fileName));
            m_PcapWriters[ipAddressValue]->open();
            TOYVPN_LOG_INFO("Created pcapng file: '" << fileName << "'");
        }

        return m_PcapWriters[ipAddressValue].get();
    }

    void removePcapWriter(const pcpp::IPv4Address& clientAddress) {
        auto ipAddressValue = clientAddress.toInt();
        if (m_PcapWriters.find(ipAddressValue) != m_PcapWriters.end()) {
            m_PcapWriters.erase(ipAddressValue);
        }
    }

    timespec getCurrentTimestamp() {
        auto now = std::chrono::system_clock::now();
        auto duration = now.time_since_epoch();
        auto seconds = std::chrono::duration_cast<std::chrono::seconds>(duration);
        auto nanoseconds = std::chrono::duration_cast<std::chrono::nanoseconds>(duration) - seconds;

        timespec result;
        result.tv_sec = seconds.count();
        result.tv_nsec = nanoseconds.count();

        return result;
    }
};