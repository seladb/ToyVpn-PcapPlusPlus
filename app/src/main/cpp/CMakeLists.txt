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
    ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/${ANDROID_ABI}/${API_VERSION}/libPcap++.a
    ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/${ANDROID_ABI}/${API_VERSION}/libPacket++.a
    ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/${ANDROID_ABI}/${API_VERSION}/libCommon++.a
    ${CMAKE_SOURCE_DIR}/../../../libs/libpcap-android/${ANDROID_ABI}/${API_VERSION}/libpcap.a
    log
    android
)
