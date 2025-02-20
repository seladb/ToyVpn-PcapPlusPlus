cmake_minimum_required(VERSION 3.20)
project(ToyVpnServer)

set(CMAKE_CXX_STANDARD 17)

set(PCAPPLUSPLUS_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/libs/pcapplusplus/include")
set(PCAPPLUSPLUS_LIB_DIR "${CMAKE_SOURCE_DIR}/libs/pcapplusplus/lib")

# Create the executable target first
add_executable(ToyVpnServer
        ToyVpnServer.h
        main.cpp)

# Add the argparse directory to the target's include path
target_include_directories(ToyVpnServer PRIVATE argparse)

# Add PcapPlusPlus headers to the include search path
target_include_directories(ToyVpnServer PRIVATE ${PCAPPLUSPLUS_INCLUDE_DIR})

# Link the necessary PcapPlusPlus libraries
target_link_libraries(ToyVpnServer PRIVATE
        ${PCAPPLUSPLUS_LIB_DIR}/libPcap++.a
        ${PCAPPLUSPLUS_LIB_DIR}/libPacket++.a
        ${PCAPPLUSPLUS_LIB_DIR}/libCommon++.a
        pcap)
