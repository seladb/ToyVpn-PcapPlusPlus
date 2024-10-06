cmake_minimum_required(VERSION 3.10.2)

project("pcapplusplus-interface")

find_package(PcapPlusPlus REQUIRED)

add_library(
        pcapplusplus-interface
        SHARED
        pcapplusplus_interface.cpp)


find_library(log-lib log)

target_link_libraries(
        pcapplusplus-interface
        PRIVATE
        PcapPlusPlus::Pcap++
        ${log-lib})
