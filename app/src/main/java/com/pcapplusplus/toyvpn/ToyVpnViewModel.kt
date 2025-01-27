package com.pcapplusplus.toyvpn

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pcapplusplus.toyvpn.model.DomainData
import com.pcapplusplus.toyvpn.model.DomainTracker
import com.pcapplusplus.toyvpn.model.PacketData
import com.pcapplusplus.toyvpn.model.VpnConnectionState
import kotlinx.coroutines.launch

class ToyVpnViewModel(private val vpnServiceManager: ToyVpnServiceManager) : ViewModel(),
    PacketDataHandler {
    private val _vpnConnectionState = MutableLiveData(VpnConnectionState.DISCONNECTED)
    private val _vpnConnectionError = MutableLiveData<String?>(null)
    private val _packetCount = MutableLiveData(0)
    private val _ipv4PacketCount = MutableLiveData(0)
    private val _ipv6PacketCount = MutableLiveData(0)
    private val _tcpPacketCount = MutableLiveData(0)
    private val _udpPacketCount = MutableLiveData(0)
    private val _dnsPacketCount = MutableLiveData(0)
    private val _tlsPacketCount = MutableLiveData(0)
    private val _tcpConnectionCount = MutableLiveData(0)
    private val _udpConnectionCount = MutableLiveData(0)
    private val _topDnsDomains = MutableLiveData<List<DomainData>>()
    private val _topTlsServerNames = MutableLiveData<List<DomainData>>()

    private val tcpConnections: MutableMap<Long, Int> = mutableMapOf()
    private val udpConnections: MutableMap<Long, Int> = mutableMapOf()
    private val dnsDomainTracker =
        DomainTracker(timeWindowMillis = TOP_DNS_DOMAINS_TIME_WINDOW_MSEC)
    private val tlsServerNameTracker =
        DomainTracker(timeWindowMillis = TOP_TLS_SERVER_NAMES_TIME_WINDOW_MSEC)

    val vpnConnectionState: LiveData<VpnConnectionState> get() = _vpnConnectionState
    val vpnConnectionError: LiveData<String?> get() = _vpnConnectionError
    val packetCount: LiveData<Int> get() = _packetCount
    val ipv4PacketCount: LiveData<Int> get() = _ipv4PacketCount
    val ipv6PacketCount: LiveData<Int> get() = _ipv6PacketCount
    val tcpPacketCount: LiveData<Int> get() = _tcpPacketCount
    val udpPacketCount: LiveData<Int> get() = _udpPacketCount
    val dnsPacketCount: LiveData<Int> get() = _dnsPacketCount
    val tlsPacketCount: LiveData<Int> get() = _tlsPacketCount
    val tcpConnectionCount: LiveData<Int> get() = _tcpConnectionCount
    val udpConnectionCount: LiveData<Int> get() = _udpConnectionCount
    val topDnsDomains: LiveData<List<DomainData>> get() = _topDnsDomains
    val topTlsServerNames: LiveData<List<DomainData>> get() = _topTlsServerNames

    init {
        viewModelScope.launch {
            vpnServiceManager.vpnServiceState.collect { vpnServiceState ->
                Log.w("ToyVpnViewModel", "Updating _vpnConnected.value to $vpnServiceState !!!!!!!")
                _vpnConnectionState.value = vpnServiceState
            }
        }
        viewModelScope.launch {
            vpnServiceManager.vpnConnectionError.collect { errorMessage ->
                _vpnConnectionError.value = errorMessage
                Log.w("ToyVpnViewModel", "Got error message: $errorMessage")
            }
        }
        vpnServiceManager.registerPacketDataHandler(this)
    }

    companion object {
        const val TOP_DNS_DOMAINS_COUNT = 10
        const val TOP_DNS_DOMAINS_TIME_WINDOW_MSEC = 5 * 60 * 1000
        const val TOP_TLS_SERVER_NAMES_COUNT = 10
        const val TOP_TLS_SERVER_NAMES_TIME_WINDOW_MSEC = 5 * 60 * 1000
    }

    fun connectVpn(serverAddress: String, serverPort: Int, secret: String) {
        _packetCount.postValue(0)
        _ipv4PacketCount.postValue(0)
        _ipv6PacketCount.postValue(0)
        _udpPacketCount.postValue(0)
        _tcpPacketCount.postValue(0)
        _dnsPacketCount.postValue(0)
        _tlsPacketCount.postValue(0)
        _tcpConnectionCount.postValue(0)
        _udpConnectionCount.postValue(0)
        tcpConnections.clear()
        udpConnections.clear()
        dnsDomainTracker.clear()
        _topDnsDomains.postValue(listOf())
        tlsServerNameTracker.clear()
        _topTlsServerNames.postValue(listOf())

        viewModelScope.launch {
            Log.w("ToyVpnViewModel", "Running vpnServiceManager.startVpnService !!!!!!!")
            vpnServiceManager.startVpnService(serverAddress, serverPort, secret)
        }
    }

    fun disconnectVpn() {
        viewModelScope.launch {
            Log.w("ToyVpnViewModel", "Running vpnServiceManager.stopVpnService !!!!!!!")
            vpnServiceManager.stopVpnService()
        }
    }

    override fun onPacketDataArrives(packetDataList: ArrayList<PacketData>) {
        Log.w("ToyVpnViewModel", "handling packetDataList of size ${packetDataList.size}")
        var ipv4Count = 0
        var ipv6Count = 0
        var tcpCount = 0
        var udpCount = 0
        var dnsCount = 0
        var tlsCount = 0
        for (packetData in packetDataList) {
            if (packetData.isUDP) {
                udpCount++
                if (packetData.connectionID != null) {
                    udpConnections[packetData.connectionID] =
                        udpConnections.getOrDefault(packetData.connectionID, 0) + 1
                }
            }
            if (packetData.isTCP) {
                tcpCount++
                if (packetData.connectionID != null) {
                    tcpConnections[packetData.connectionID] =
                        tcpConnections.getOrDefault(packetData.connectionID, 0) + 1
                }
            }
            if (packetData.isIPv4) {
                ipv4Count++
            }
            if (packetData.isIPv6) {
                ipv6Count++
            }
            if (packetData.isDNS) {
                dnsCount++
            }
            if (packetData.isTLS) {
                tlsCount++
            }

            if (packetData.dnsQuery != null) {
                dnsDomainTracker.recordDomain(packetData.dnsQuery)
                _topDnsDomains.value = dnsDomainTracker.getTopDomains(TOP_DNS_DOMAINS_COUNT)
            }

            if (packetData.tlsServerName != null) {
                tlsServerNameTracker.recordDomain(packetData.tlsServerName)
                _topTlsServerNames.value =
                    tlsServerNameTracker.getTopDomains(TOP_TLS_SERVER_NAMES_COUNT)
            }
        }

        _packetCount.postValue(packetCount.value?.plus(packetDataList.size))
        _ipv4PacketCount.postValue(_ipv4PacketCount.value?.plus(ipv4Count))
        _ipv6PacketCount.postValue(_ipv6PacketCount.value?.plus(ipv6Count))
        _udpPacketCount.postValue(_udpPacketCount.value?.plus(udpCount))
        _tcpPacketCount.postValue(_tcpPacketCount.value?.plus(tcpCount))
        _dnsPacketCount.postValue(_dnsPacketCount.value?.plus(dnsCount))
        _tlsPacketCount.postValue(_tlsPacketCount.value?.plus(tlsCount))
        _tcpConnectionCount.postValue(tcpConnections.size)
        _udpConnectionCount.postValue(udpConnections.size)
    }
}
