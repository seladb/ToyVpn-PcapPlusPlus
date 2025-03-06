package com.pcapplusplus.toyvpn

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pcapplusplus.toyvpn.model.DomainData
import com.pcapplusplus.toyvpn.model.PacketData
import com.pcapplusplus.toyvpn.model.VpnConnectionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToyVpnViewModelTest {
    private val mockVpnServiceManager = mockk<ToyVpnServiceManager>(relaxed = true)
    private var vpnConnectionState = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    private var clientAddress = MutableStateFlow<String?>(null)
    private var vpnConnectionError = MutableStateFlow<String?>(null)
    private lateinit var viewModel: ToyVpnViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        every { mockVpnServiceManager.vpnServiceState } returns vpnConnectionState.asStateFlow()
        every { mockVpnServiceManager.clientAddress } returns clientAddress.asStateFlow()
        every { mockVpnServiceManager.vpnConnectionError } returns vpnConnectionError.asStateFlow()
        viewModel = ToyVpnViewModel(mockVpnServiceManager)
    }

    @Test
    fun testConnectVpn() {
        viewModel.onPacketDataArrives(arrayListOf(PacketData(isIPv4 = true, length = 10)))
        assertEquals(1, viewModel.ipv4PacketCount.value)

        val mockClientAddress = "10.0.0.1"
        clientAddress.value = mockClientAddress
        val serverAddress = "12.13.14.15"
        val serverPort = 8001
        val secret = "secret"

        viewModel.connectVpn(serverAddress, serverPort, secret)

        assertEquals(0, viewModel.packetCount.value)
        assertEquals(0, viewModel.ipv4PacketCount.value)
        assertEquals(0, viewModel.ipv6PacketCount.value)
        assertEquals(0, viewModel.udpPacketCount.value)
        assertEquals(0, viewModel.tcpPacketCount.value)
        assertEquals(0, viewModel.dnsPacketCount.value)
        assertEquals(0, viewModel.tlsPacketCount.value)
        assertEquals(0, viewModel.tcpConnectionCount.value)
        assertEquals(0, viewModel.udpConnectionCount.value)
        assertEquals(0, viewModel.topDnsDomains.value?.size)
        assertEquals(0, viewModel.topTlsServerNames.value?.size)

        assertEquals(VpnConnectionState.DISCONNECTED, viewModel.vpnConnectionState.value)
        assertNull(viewModel.vpnConnectionError.value)

        verify { mockVpnServiceManager.startVpnService(serverAddress, serverPort, secret) }
    }

    @Test
    fun testDisconnectVpn() =
        runTest {
            viewModel.onPacketDataArrives(arrayListOf(PacketData(isIPv4 = true, length = 10)))
            assertEquals(1, viewModel.ipv4PacketCount.value)

            viewModel.disconnectVpn()

            verify(timeout = 10000) { mockVpnServiceManager.stopVpnService() }

            assertEquals(1, viewModel.ipv4PacketCount.value)
        }

    @Test
    fun testPacketsArrive() {
        assertEquals(0, viewModel.packetCount.value)
        assertEquals(0, viewModel.ipv4PacketCount.value)
        assertEquals(0, viewModel.ipv6PacketCount.value)
        assertEquals(0, viewModel.udpPacketCount.value)
        assertEquals(0, viewModel.tcpPacketCount.value)
        assertEquals(0, viewModel.dnsPacketCount.value)
        assertEquals(0, viewModel.tlsPacketCount.value)

        viewModel.onPacketDataArrives(arrayListOf(PacketData(isIPv4 = true, length = 10)))
        assertEquals(1, viewModel.ipv4PacketCount.value)
        assertEquals(1, viewModel.packetCount.value)

        viewModel.onPacketDataArrives(arrayListOf(PacketData(isIPv6 = true, length = 10)))
        assertEquals(1, viewModel.ipv6PacketCount.value)
        assertEquals(2, viewModel.packetCount.value)

        viewModel.onPacketDataArrives(arrayListOf(PacketData(isTCP = true, length = 10)))
        assertEquals(1, viewModel.tcpPacketCount.value)
        assertEquals(3, viewModel.packetCount.value)

        viewModel.onPacketDataArrives(arrayListOf(PacketData(isUDP = true, length = 10)))
        assertEquals(1, viewModel.udpPacketCount.value)
        assertEquals(4, viewModel.packetCount.value)

        viewModel.onPacketDataArrives(arrayListOf(PacketData(isDNS = true, length = 10)))
        assertEquals(1, viewModel.dnsPacketCount.value)
        assertEquals(5, viewModel.packetCount.value)

        viewModel.onPacketDataArrives(arrayListOf(PacketData(isTLS = true, length = 10)))
        assertEquals(1, viewModel.dnsPacketCount.value)
        assertEquals(6, viewModel.packetCount.value)

        viewModel.onPacketDataArrives(
            arrayListOf(
                PacketData(
                    isTCP = true,
                    connectionID = 1L,
                    length = 10,
                ),
            ),
        )
        assertEquals(1, viewModel.tcpConnectionCount.value)
        viewModel.onPacketDataArrives(
            arrayListOf(
                PacketData(
                    isTCP = true,
                    connectionID = 1L,
                    length = 10,
                ),
            ),
        )
        assertEquals(1, viewModel.tcpConnectionCount.value)
        viewModel.onPacketDataArrives(
            arrayListOf(
                PacketData(
                    isTCP = true,
                    connectionID = 2L,
                    length = 10,
                ),
            ),
        )
        assertEquals(2, viewModel.tcpConnectionCount.value)

        viewModel.onPacketDataArrives(
            arrayListOf(
                PacketData(
                    isUDP = true,
                    connectionID = 1L,
                    length = 10,
                ),
            ),
        )
        assertEquals(1, viewModel.udpConnectionCount.value)
        viewModel.onPacketDataArrives(
            arrayListOf(
                PacketData(
                    isUDP = true,
                    connectionID = 1L,
                    length = 10,
                ),
            ),
        )
        assertEquals(1, viewModel.udpConnectionCount.value)
        viewModel.onPacketDataArrives(
            arrayListOf(
                PacketData(
                    isUDP = true,
                    connectionID = 2L,
                    length = 10,
                ),
            ),
        )
        assertEquals(2, viewModel.udpConnectionCount.value)

        viewModel.onPacketDataArrives(arrayListOf(PacketData(dnsQuery = "test.com", length = 10)))
        assertEquals(listOf(DomainData("test.com", 1)), viewModel.topDnsDomains.value)

        viewModel.onPacketDataArrives(
            arrayListOf(
                PacketData(
                    tlsServerName = "test.com",
                    length = 10,
                ),
            ),
        )
        assertEquals(listOf(DomainData("test.com", 1)), viewModel.topTlsServerNames.value)

        viewModel.onPacketDataArrives(
            arrayListOf(
                PacketData(isIPv4 = true, length = 10),
                PacketData(isIPv4 = true, length = 10),
            ),
        )
        assertEquals(3, viewModel.ipv4PacketCount.value)

        assertEquals(16, viewModel.packetCount.value)
    }

    @Test
    fun testVpnConnectionStateChange() =
        runTest {
            assertEquals(VpnConnectionState.DISCONNECTED, viewModel.vpnConnectionState.value)

            vpnConnectionState.value = VpnConnectionState.CONNECTED
            waitFor { viewModel.vpnConnectionState.value == VpnConnectionState.CONNECTED }
        }

    @Test
    fun testVpnConnectionError() =
        runTest {
            assertNull(viewModel.vpnConnectionError.value)

            vpnConnectionError.value = "Some error"
            waitFor { viewModel.vpnConnectionError.value == "Some error" }
        }

    @Test
    fun testClientAddress() =
        runTest {
            assertNull(viewModel.clientAddress.value)

            clientAddress.value = "10.0.0.1"
            waitFor { viewModel.clientAddress.value == "10.0.0.1" }
        }
}
