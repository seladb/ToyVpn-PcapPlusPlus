package com.pcapplusplus.toyvpn.pcapplusplus

import android.util.Log
import com.pcapplusplus.toyvpn.model.PacketData
import kotlinx.serialization.json.Json

class PacketProcessor(private val pcapPlusPlusInterface: PcapPlusPlusInterface = PcapPlusPlusInterface()) {
    fun processPacket(rawPacketData: ByteArray): PacketData? {
        try {
            val result = String(pcapPlusPlusInterface.analyzePacket(rawPacketData))
            return Json.decodeFromString<PacketData>(result)
        } catch (e: Exception) {
            Log.e("ToyVpnPacketAnalyzer", "Error in processing packet", e)
            return null
        }
    }
}