package com.pcapplusplus.toyvpn

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSibling
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.MutableLiveData
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pcapplusplus.toyvpn.model.DomainData
import com.pcapplusplus.toyvpn.model.VpnConnectionState
import com.pcapplusplus.toyvpn.ui.theme.ToyVpnPcapPlusPlusTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StatsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: ToyVpnViewModel

    private fun renderScreen(
        vpnConnectionState: VpnConnectionState = VpnConnectionState.CONNECTED,
        clientAddress: String? = null,
        totalPacketCount: Int = 0,
        ipv4PacketCount: Int = 0,
        ipv6PacketCount: Int = 0,
        tcpPacketCount: Int = 0,
        udpPacketCount: Int = 0,
        dnsPacketCount: Int = 0,
        tlsPacketCount: Int = 0,
        tcpConnectionCount: Int = 0,
        udpConnectionCount: Int = 0,
        topDnsDomains: List<DomainData> = listOf(),
        topTlsServerNames: List<DomainData> = listOf(),
    ) {
        val vpnConnectionStateLiveData = MutableLiveData(vpnConnectionState)
        val clientAddressLiveData = MutableLiveData<String?>(clientAddress)
        val topDnsDomainsLiveData = MutableLiveData(topDnsDomains)
        val topTlsServerNamesLiveData = MutableLiveData(topTlsServerNames)
        val packetCountLiveData = MutableLiveData(totalPacketCount)
        val ipv4PacketCountLiveData = MutableLiveData(ipv4PacketCount)
        val ipv6PacketCountLiveData = MutableLiveData(ipv6PacketCount)
        val tcpPacketCountLiveData = MutableLiveData(tcpPacketCount)
        val udpPacketCountLiveData = MutableLiveData(udpPacketCount)
        val dnsPacketCountLiveData = MutableLiveData(dnsPacketCount)
        val tlsPacketCountLiveData = MutableLiveData(tlsPacketCount)
        val tcpConnectionsLiveData = MutableLiveData(tcpConnectionCount)
        val udpConnectionsLiveData = MutableLiveData(udpConnectionCount)

        every { mockViewModel.vpnConnectionState } returns vpnConnectionStateLiveData
        every { mockViewModel.clientAddress } returns clientAddressLiveData
        every { mockViewModel.topDnsDomains } returns topDnsDomainsLiveData
        every { mockViewModel.topTlsServerNames } returns topTlsServerNamesLiveData
        every { mockViewModel.packetCount } returns packetCountLiveData
        every { mockViewModel.ipv4PacketCount } returns ipv4PacketCountLiveData
        every { mockViewModel.ipv6PacketCount } returns ipv6PacketCountLiveData
        every { mockViewModel.tcpPacketCount } returns tcpPacketCountLiveData
        every { mockViewModel.udpPacketCount } returns udpPacketCountLiveData
        every { mockViewModel.dnsPacketCount } returns dnsPacketCountLiveData
        every { mockViewModel.tlsPacketCount } returns tlsPacketCountLiveData
        every { mockViewModel.tcpConnectionCount } returns tcpConnectionsLiveData
        every { mockViewModel.udpConnectionCount } returns udpConnectionsLiveData

        composeTestRule.setContent {
            ToyVpnPcapPlusPlusTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "stats_screen",
                ) {
                    composable("stats_screen") {
                        StatsScreen(navController, mockViewModel)
                    }
                    composable("connect_screen") {
                        Text("Connect Screen")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Before
    fun setUp() {
        mockViewModel = mockk(relaxed = true)
    }

    @Test
    fun testVpnConnected() {
        renderScreen()

        val buttonNode = composeTestRule.onNodeWithText("Disconnect")
        buttonNode.performScrollTo()
        composeTestRule.waitForIdle()
        buttonNode.assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun testVpnConnectedWithClientAddress() {
        renderScreen(clientAddress = "1.2.3.4")

        composeTestRule.onNodeWithText("IP Address").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.2.3.4").assertIsDisplayed()
    }

    @Test
    fun testVpnConnectedWithPacketTraffic() {
        val totalPacketCount = 396
        val ipv4PacketCount = 11
        val ipv6PacketCount = 22
        val tcpPacketCount = 33
        val udpPacketCount = 44
        val dnsPacketCount = 55
        val tlsPacketCount = 66
        val tcpConnectionCount = 77
        val udpConnectionCount = 88

        renderScreen(
            totalPacketCount = totalPacketCount,
            ipv4PacketCount = ipv4PacketCount,
            ipv6PacketCount = ipv6PacketCount,
            tcpPacketCount = tcpPacketCount,
            udpPacketCount = udpPacketCount,
            dnsPacketCount = dnsPacketCount,
            tlsPacketCount = tlsPacketCount,
            tcpConnectionCount = tcpConnectionCount,
            udpConnectionCount = udpConnectionCount,
        )

        val expectedValues =
            listOf(
                Triple("IPv4", "IPv4", ipv4PacketCount),
                Triple("IPv6", "IPv6", ipv6PacketCount),
                Triple("TCP", "TCP", tcpPacketCount),
                Triple("UDP", "UDP", udpPacketCount),
                Triple("DNS", "DNS", dnsPacketCount),
                Triple("TLS", "TLS", tlsPacketCount),
                Triple("TCPConn", "TCP", tcpConnectionCount),
                Triple("UDPConn", "UDP", udpConnectionCount),
            )

        composeTestRule.onNodeWithText("Total Packets").onSibling().assertTextEquals(totalPacketCount.toString())
        expectedValues.forEach { (testTag, label, count) ->
            composeTestRule.onNodeWithTag("${testTag}_label").assertTextEquals(label)
            composeTestRule.onNodeWithTag("${testTag}_count").assertTextEquals(count.toString())
            composeTestRule.onNodeWithTag("${testTag}_progress").assertExists()
        }
    }

    @Test
    fun testHumanizePacketCount() {
        val totalPacketCount = 996099
        val ipv4PacketCount = 123
        val ipv6PacketCount = 1234
        val tcpPacketCount = 12_345
        val udpPacketCount = 122_000
        val dnsPacketCount = 4_599_888
        val tlsPacketCount = 34_829_007
        val tcpConnectionCount = 999_999_999
        val udpConnectionCount = 1_000_990_000

        renderScreen(
            totalPacketCount = totalPacketCount,
            ipv4PacketCount = ipv4PacketCount,
            ipv6PacketCount = ipv6PacketCount,
            tcpPacketCount = tcpPacketCount,
            udpPacketCount = udpPacketCount,
            dnsPacketCount = dnsPacketCount,
            tlsPacketCount = tlsPacketCount,
            tcpConnectionCount = tcpConnectionCount,
            udpConnectionCount = udpConnectionCount,
        )

        val expectedValues =
            listOf(
                Pair("IPv4", "123"),
                Pair("IPv6", "1234"),
                Pair("TCP", "12.35K"),
                Pair("UDP", "122K"),
                Pair("DNS", "4.6M"),
                Pair("TLS", "34.83M"),
                Pair("TCPConn", "1B"),
                Pair("UDPConn", "1.001B"),
            )

        composeTestRule.onNodeWithText("Total Packets").onSibling().assertTextEquals(totalPacketCount.toString())
        expectedValues.forEach { (testTag, humanizedCount) ->
            composeTestRule.onNodeWithTag("${testTag}_count").assertTextEquals(humanizedCount)
        }
    }

    @Test
    fun testVpnConnectedWithTopDnsDomainData() {
        val topDnsDomains =
            listOf(
                DomainData("google.com", 11),
                DomainData("example.com", 22),
            )

        renderScreen(topDnsDomains = topDnsDomains)

        topDnsDomains.forEach { (domain, count) ->
            composeTestRule.onNodeWithText("https://$domain")
            composeTestRule.onNodeWithText(count.toString())
        }
    }

    @Test
    fun testVpnConnectedWithTopTlsServerNamesData() {
        val topTlsServerNames =
            listOf(
                DomainData("facebook.com", 33),
                DomainData("apple.com", 44),
            )

        renderScreen(topTlsServerNames = topTlsServerNames)

        topTlsServerNames.forEach { (domain, count) ->
            composeTestRule.onNodeWithText("https://$domain")
            composeTestRule.onNodeWithText(count.toString())
        }
    }

    @Test
    fun testClickDisconnectButton() {
        renderScreen()

        val buttonNode = composeTestRule.onNodeWithText("Disconnect")

        buttonNode.performScrollTo()
        composeTestRule.waitForIdle()
        buttonNode.performClick()

        verify { mockViewModel.disconnectVpn() }
    }

    @Test
    fun testVpnDisconnecting() {
        renderScreen(vpnConnectionState = VpnConnectionState.DISCONNECTING)

        val buttonNode = composeTestRule.onNodeWithText("Disconnecting...")

        buttonNode.performScrollTo()
        composeTestRule.waitForIdle()
        buttonNode.assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun testVpnDisconnected() {
        renderScreen(vpnConnectionState = VpnConnectionState.DISCONNECTED)

        composeTestRule.onNodeWithText("Connect Screen").assertIsDisplayed()
    }
}
