cmake_minimum_required(VERSION 3.4.1)

project("pcapplusplus_interface")

add_library(pcapplusplus_interface SHARED PacketAnalyzer.cpp)

include_directories(
    pcapplusplus_interface
    PUBLIC
    ${CMAKE_SOURCE_DIR}/../../../libs/libpcap-android/include
)
include_directories(
    pcapplusplus_interface
    PUBLIC
    ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/include
)

set(API_VERSION ${ANDROID_PLATFORM})
string(REPLACE "android-" "" API_VERSION ${API_VERSION})

target_link_libraries(
    pcapplusplus_interface
    ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/${ANDROID_ABI}/30/libPcap++.a
    ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/${ANDROID_ABI}/30/libPacket++.a
    ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/${ANDROID_ABI}/30/libCommon++.a
    ${CMAKE_SOURCE_DIR}/../../../libs/libpcap-android/${ANDROID_ABI}/30/libpcap.a
    log
    android
)
