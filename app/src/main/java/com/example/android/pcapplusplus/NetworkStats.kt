package com.example.android.pcapplusplus

import java.util.Collections.emptyMap

data class NetworkStats(var packetCount: Int = 0) {
    var ipv4Count: Int = 0
    var ipv6Count: Int = 0
    var tcpCount: Int = 0
    var udpCount: Int = 0
    var dnsRequestCount: Int = 0
    var dnsResponseCount: Int = 0
    var tlsVersions = HashMap<String, Int>()
    var tlsSNI =  HashMap<String, Int>()
    var connectionMap = HashMap<Int, Boolean>()

    override fun toString(): String {
        val top3TLSVersion = tlsVersions.toList()
                .sortedByDescending { (_, value) -> value }
                .take(3)
        val top5SNI = tlsSNI.toList()
                .sortedByDescending { (_, value) -> value }
                .take(5)
        return "Packets=${packetCount}\nIPv4=${ipv4Count}\nIPv6=${ipv6Count}\nTCP=${tcpCount}\nUDP=${udpCount}\nConnections=${connectionMap.size}\nDNS_req=${dnsRequestCount}\nDNS_res=${dnsResponseCount}\nTop_TLS_Version=${top3TLSVersion}\nTOP_SNI=${top5SNI}"
    }
}