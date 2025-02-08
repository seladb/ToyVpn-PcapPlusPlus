package com.pcapplusplus.toyvpn

import android.content.Intent
import android.net.VpnService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pcapplusplus.toyvpn.model.BroadcastActions
import com.pcapplusplus.toyvpn.model.VpnSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.InvocationTargetException
import java.nio.channels.DatagramChannel

@RunWith(AndroidJUnit4::class)
class ToyVpnServiceEstablishVpnConnectionTest {
    private val vpnService = spyk(ToyVpnService(), recordPrivateCalls = true)
    private val mockDatagramChannel = mockk<DatagramChannel>()
    private val establishVpnConnectionMethod =
        vpnService::class.java.getDeclaredMethod(
            "establishVpnConnection",
            String::class.java,
            Int::class.java,
            String::class.java,
        ).apply {
            isAccessible = true
        }

    private fun callEstablishVpnConnection(
        serverAddress: String = "1.2.3.4",
        serverPort: Int = 8000,
        secret: String = "secret",
    ): DatagramChannel {
        val parameters = arrayOf<Any>(serverAddress, serverPort, secret)
        try {
            return establishVpnConnectionMethod.invoke(
                vpnService,
                *parameters,
            ) as DatagramChannel
        } catch (ex: InvocationTargetException) {
            throw ex.cause!!
        }
    }

    @Test
    fun testSuccess() {
        val vpnSettings =
            VpnSettings(
                clientAddress = "1.2.3.4",
                clientAddressPrefixLength = 24,
                routeAddress = "0.0.0.0",
                routePrefixLength = 0,
                mtu = 1400,
                dnsServer = "8.8.8.8",
            )
        every { vpnService.sendBroadcast(any<Intent>()) } answers {}
        every {
            vpnService["handshake"](
                any<String>(),
                any<Int>(),
                any<String>(),
                any<Boolean>(),
            )
        } answers {
            Pair(mockDatagramChannel, vpnSettings)
        }

        val mockBuilder = mockk<VpnService.Builder>(relaxed = true)
        mockkConstructor(VpnService.Builder::class)
        every { anyConstructed<VpnService.Builder>().setSession(any<String>()) } returns mockBuilder
        every {
            anyConstructed<VpnService.Builder>().addAddress(
                any<String>(),
                any<Int>(),
            )
        } returns mockBuilder
        every { anyConstructed<VpnService.Builder>().setMtu(any<Int>()) } returns mockBuilder
        every {
            anyConstructed<VpnService.Builder>().addRoute(
                any<String>(),
                any<Int>(),
            )
        } returns mockBuilder
        every { anyConstructed<VpnService.Builder>().addDnsServer(any<String>()) } returns mockBuilder
        every { anyConstructed<VpnService.Builder>().establish() } returns null

        assertEquals(mockDatagramChannel, callEstablishVpnConnection())

        verify { vpnService["handshake"]("1.2.3.4", 8000, "secret", true) }
        verify {
            anyConstructed<VpnService.Builder>().setSession("ToyVPN")
            anyConstructed<VpnService.Builder>().addAddress(
                vpnSettings.clientAddress,
                vpnSettings.clientAddressPrefixLength,
            )
            anyConstructed<VpnService.Builder>().setMtu(vpnSettings.mtu)
            anyConstructed<VpnService.Builder>().addRoute(
                vpnSettings.routeAddress,
                vpnSettings.routePrefixLength,
            )
            anyConstructed<VpnService.Builder>().addDnsServer("8.8.8.8")
            anyConstructed<VpnService.Builder>().establish()
        }
        verify { vpnService.sendBroadcast(match { it.action == BroadcastActions.VPN_SERVICE_STARTED }) }
    }
}
