package com.pcapplusplus.toyvpn.pcapplusplus

import android.util.Log

class PcapPlusPlusInterface {
    init {
        try {
            System.loadLibrary("pcapplusplus_interface")
        } catch (e: Exception) {
            Log.e("ToyVpnPacketAnalyzer", "Error loading pcapplusplus", e)
        }
    }

    private external fun analyzePacketNative(rawPacketData: ByteArray): ByteArray

    fun analyzePacket(rawPacketData: ByteArray): ByteArray {
        return analyzePacketNative(rawPacketData)
    }
}
