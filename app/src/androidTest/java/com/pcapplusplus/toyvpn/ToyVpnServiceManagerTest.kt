package com.pcapplusplus.toyvpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.result.ActivityResultLauncher
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pcapplusplus.toyvpn.model.BroadcastActions
import com.pcapplusplus.toyvpn.model.PacketData
import com.pcapplusplus.toyvpn.model.VpnConnectionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToyVpnServiceManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mockActivityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
    private val mockVpnServiceProxy = mockk<DefaultVpnServiceProxy>(relaxed = true)
    private val packetDataHandler = object : PacketDataHandler {
        val packetDatasArrived = ArrayList<PacketData>()
        override fun onPacketDataArrives(packetDataList: ArrayList<PacketData>) {
            packetDatasArrived.addAll(packetDataList)
        }
    }

    private lateinit var serviceManager: ToyVpnServiceManager

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        every { mockVpnServiceProxy.prepare(any()) } returns null
        serviceManager =
            ToyVpnServiceManager(context, mockActivityResultLauncher, mockVpnServiceProxy)
        serviceManager.registerPacketDataHandler(packetDataHandler)
    }

    @After
    fun tearDown() {
        serviceManager.close()
    }

    @Test
    fun testActivityResultLauncherCalledOnInit() {
        val prepareIntent = mockk<Intent>()
        every { mockVpnServiceProxy.prepare(any()) } returns prepareIntent

        ToyVpnServiceManager(context, mockActivityResultLauncher, mockVpnServiceProxy)

        verify(exactly = 1) { mockActivityResultLauncher.launch(prepareIntent) }
    }

    @Test
    fun testActivityResultLauncherNotCalledIfNotNeeded() {
        verify(exactly = 0) { mockActivityResultLauncher.launch(any()) }
    }

    @Test
    fun testStartVpnService() {
        val serverAddress = "1.2.3.4"
        val serverPort = 8000
        val secret = "secret"

        serviceManager.startVpnService(serverAddress, serverPort, secret)
        assertEquals(VpnConnectionState.CONNECTING, serviceManager.vpnServiceState.value)
        assertNull(serviceManager.vpnConnectionError.value)

        verify {
            mockVpnServiceProxy.startVpnService(withArg {
                assertEquals(serverAddress, it.getStringExtra("serverAddress"))
                assertEquals(serverPort, it.getIntExtra("serverPort", 0))
                assertEquals(secret, it.getStringExtra("serverSecret"))
            })
        }
    }

    @Test
    fun testStopVpnService() = runTest {
        val receiver = object : BroadcastReceiver() {
            var gotStopVpnIntent = false
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BroadcastActions.VPN_SERVICE_STOP) {
                    gotStopVpnIntent = true
                }
            }
        }

        try {
            context.registerReceiver(
                receiver,
                IntentFilter(BroadcastActions.VPN_SERVICE_STOP),
                Context.RECEIVER_EXPORTED
            )

            serviceManager.stopVpnService()
            assertEquals(VpnConnectionState.DISCONNECTING, serviceManager.vpnServiceState.value)
            assertNull(serviceManager.vpnConnectionError.value)

            waitFor { receiver.gotStopVpnIntent }
        } finally {
            context.unregisterReceiver(receiver)

        }
    }

    @Test
    fun testReceiveVpnStartedAndStoppedEvent() = runTest {
        context.sendBroadcast(Intent(BroadcastActions.VPN_SERVICE_STARTED))
        waitFor { serviceManager.vpnServiceState.value == VpnConnectionState.CONNECTED }

        context.sendBroadcast(Intent(BroadcastActions.VPN_SERVICE_STOPPED))
        waitFor { serviceManager.vpnServiceState.value == VpnConnectionState.DISCONNECTED }
    }

    @Test
    fun testReceiveVpnErrorEvent() = runTest {
        context.sendBroadcast(Intent(BroadcastActions.VPN_SERVICE_STARTED))
        waitFor { serviceManager.vpnServiceState.value == VpnConnectionState.CONNECTED }
        assertNull(serviceManager.vpnConnectionError.value)

        val errorMessage = "Some error"
        val errorIntent = Intent(BroadcastActions.VPN_SERVICE_ERROR).apply {
            putExtra("errorMessage", errorMessage)
        }

        context.sendBroadcast(errorIntent)

        waitFor { serviceManager.vpnServiceState.value == VpnConnectionState.DISCONNECTED }
        waitFor { serviceManager.vpnConnectionError.value == errorMessage }
    }

    @Test
    fun testReceivePacketArrivedEvent() = runTest {
        val packetDatas = arrayListOf(
            PacketData(isIPv4 = true, length = 10),
            PacketData(isTCP = true, length = 100)
        )
        val packetArrivedIntent = Intent(BroadcastActions.VPN_SERVICE_PACKET_ARRIVED).apply {
            putParcelableArrayListExtra("packetData", packetDatas)
        }

        context.sendBroadcast(packetArrivedIntent)

        waitFor { packetDataHandler.packetDatasArrived == packetDatas }
    }
}