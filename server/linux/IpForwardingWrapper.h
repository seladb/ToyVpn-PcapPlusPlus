#pragma once

#include <fstream>
#include "Log.h"

class IpForwardingWrapper {
public:
    virtual ~IpForwardingWrapper() {
        if (m_IsInitialized && m_InitialValue != m_Value) {
            writeToFile(m_InitialValue);
        }
    }
    void init() {
        m_InitialValue = readFromFile();
        if (m_InitialValue != m_Value) {
            writeToFile(m_Value);
        }
        m_IsInitialized = true;
    }

private:
    std::string m_InitialValue;
    bool m_IsInitialized = false;

    constexpr static std::string_view m_FilePath = "/proc/sys/net/ipv4/ip_forward";
    constexpr static std::string_view m_Value = "1";

    void writeToFile(const std::string_view& value) {
        std::ofstream file(m_FilePath.data());

        if (file.is_open()) {
            file << value;
            file.close();
            TOYVPN_LOG_DEBUG("Writing '" << value << "' to '" << m_FilePath << "'");
        } else {
            throw std::runtime_error("Cannot modify '" + std::string(m_FilePath) + "'");
        }
    }

    std::string readFromFile() {
        std::ifstream file(m_FilePath.data());

        if (!file.is_open()) {
            throw std::runtime_error("Cannot open '" + std::string(m_FilePath) + "'");
        }

        std::string value;
        std::getline(file, value);

        if (!file) {
            throw std::runtime_error("Failed to read the value from '" + std::string(m_FilePath) + "'");
        }

        return value;
    }
};