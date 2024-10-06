cmake_minimum_required(VERSION 3.10.2)

project("pcapplusplus-interface")

# Android log library
find_library(log-lib REQUIRED log)

# Find PcapPlusPlus library
find_package(PcapPlusPlus REQUIRED)

add_library(
        pcapplusplus-interface
        SHARED
        pcapplusplus_interface.cpp)

target_link_libraries(
        pcapplusplus-interface
        PRIVATE
        PcapPlusPlus::Pcap++
        log-lib)
