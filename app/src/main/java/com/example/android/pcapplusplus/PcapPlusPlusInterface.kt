package com.example.android.pcapplusplus

import android.util.Log
import org.json.JSONObject
import java.nio.ByteBuffer
import java.time.Instant

object PcapPlusPlusInterface {
    private val TAG = PcapPlusPlusInterface::class.simpleName
    private var stats: NetworkStats = NetworkStats()
    private var timestamp: Instant = Instant.now()
    private external fun analyzePacketNative(packet: ByteArray, packetLength: Int) : String
    private external fun openPcapFileNative(filesDir: String);
    private external fun closePcapFileNative();

    fun openPcapFile(filesDir: String) {
        openPcapFileNative(filesDir)
    }

    fun closePcapFile() {
        closePcapFileNative()
    }

    fun analyzePacket(packet: ByteBuffer) {
        if (Instant.now().epochSecond > timestamp.epochSecond + 5) {
            Log.i(TAG, "Packet stats:\n" + stats.toString())
            timestamp = Instant.now()
        }
        val dataAsString = analyzePacketNative(packet.array(), packet.position())
        stats.packetCount++
        val jsonObj = JSONObject(dataAsString)
        if (jsonObj.has("connectionID")) {
            val connectionID = jsonObj.getInt("connectionID")
            if (!stats.connectionMap.containsKey(connectionID)) {
                stats.connectionMap[connectionID] = true
            }
        }
        if (jsonObj.has("ipv4")) {
            stats.ipv4Count++
        }
        if (jsonObj.has("ipv6")) {
            stats.ipv6Count++
        }
        if (jsonObj.has("tcp")) {
            stats.tcpCount++
        }
        if (jsonObj.has("udp")) {
            stats.udpCount++
        }
        if (jsonObj.has("dns")) {
            val dnsType = jsonObj.getString("dns")
            if (dnsType == "dnsRequest") {
                stats.dnsRequestCount++
            }
            else if (dnsType == "dnsResponse") {
                stats.dnsResponseCount++
            }
        }
        if (jsonObj.has("tls")) {
            val tlsObj = jsonObj.getJSONObject("tls")
            if (tlsObj.has("version")) {
                val tlsVersion = tlsObj.getString("version")
                when (val tlsVersionCount = stats.tlsVersions[tlsVersion]) {
                    null -> stats.tlsVersions[tlsVersion] = 1
                    else -> stats.tlsVersions[tlsVersion] = tlsVersionCount + 1
                }
            }
            if (tlsObj.has("sni")) {
                val tlsSNI = tlsObj.getString("sni")
                when (val tlsSNICount = stats.tlsSNI[tlsSNI]) {
                    null -> stats.tlsSNI[tlsSNI] = 1
                    else -> stats.tlsSNI[tlsSNI] = tlsSNICount + 1
                }
            }
        }
    }

    init {
        System.loadLibrary("pcapplusplus-interface")
    }
}