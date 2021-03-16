cmake_minimum_required(VERSION 3.10.2)

project("pcapplusplus-interface")

add_library(
        pcapplusplus-interface
        SHARED
        pcapplusplus_interface.cpp)

find_library(log-lib log)

target_include_directories(pcapplusplus-interface PUBLIC ${CMAKE_SOURCE_DIR}/../../../libs/libpcap-android/include)
target_include_directories(pcapplusplus-interface PUBLIC ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/include)

target_link_libraries(
        pcapplusplus-interface
        ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/${ANDROID_ABI}/29/libPcap++.a
        ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/${ANDROID_ABI}/29/libPacket++.a
        ${CMAKE_SOURCE_DIR}/../../../libs/pcapplusplus/${ANDROID_ABI}/29/libCommon++.a
        ${CMAKE_SOURCE_DIR}/../../../libs/libpcap-android/${ANDROID_ABI}/29/libpcap.a
        ${log-lib})
