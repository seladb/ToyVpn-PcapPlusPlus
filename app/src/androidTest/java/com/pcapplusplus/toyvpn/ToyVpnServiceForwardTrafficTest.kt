package com.pcapplusplus.toyvpn

import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pcapplusplus.toyvpn.model.PacketData
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ToyVpnServiceForwardTrafficTest {
    private val vpnService = spyk(ToyVpnService(), recordPrivateCalls = true)
    private val mockInputStream = mockk<FileInputStream>()
    private val mockOutputStream = mockk<FileOutputStream>()
    private val mockDatagramChannel = mockk<DatagramChannel>()
    private val forwardTrafficMethod =
        vpnService::class.java.getDeclaredMethod(
            "forwardTraffic",
            FileInputStream::class.java,
            FileOutputStream::class.java,
            DatagramChannel::class.java,
        ).apply {
            isAccessible = true
        }
    private val vpnConnected =
        vpnService::class.java.getDeclaredField("vpnConnected").apply {
            isAccessible = true
        }
    private val vpnInterface =
        vpnService::class.java.getDeclaredField("vpnInterface").apply {
            isAccessible = true
        }

    private fun callForwardTraffic() {
        val parameters = arrayOf(mockInputStream, mockOutputStream, mockDatagramChannel)
        try {
            forwardTrafficMethod.invoke(vpnService, *parameters)
        } catch (ex: InvocationTargetException) {
            throw ex.cause!!
        }
    }

    @Test
    fun testForwardTraffic() {
        var fromDeviceCounter = 0
        every { mockInputStream.read(any<ByteArray>()) } answers {
            val dataSize =
                if (fromDeviceCounter < ToyVpnService.PACKET_DATA_BATCH_SIZE) {
                    val data = "from device $fromDeviceCounter".toByteArray()
                    for (i in data.indices) {
                        firstArg<ByteArray>()[i] = data[i]
                    }
                    data.size
                } else {
                    0
                }

            fromDeviceCounter++

            if (fromDeviceCounter > ToyVpnService.PACKET_DATA_BATCH_SIZE) {
                vpnConnected.set(vpnService, AtomicBoolean(false))
            }

            dataSize
        }

        val dataSentToDevice = mutableListOf<String>()
        every { mockOutputStream.write(any<ByteArray>(), any<Int>(), any<Int>()) } answers {
            val subArray =
                firstArg<ByteArray>().copyOfRange(
                    secondArg<Int>(),
                    secondArg<Int>() + thirdArg<Int>(),
                )
            dataSentToDevice.add(String(subArray))
        }

        var fromTunnelCounter = 0
        every { mockDatagramChannel.read(any<ByteBuffer>()) } answers {
            val dataSize =
                if (fromTunnelCounter < ToyVpnService.PACKET_DATA_BATCH_SIZE) {
                    val data = "from tunnel $fromTunnelCounter".toByteArray()
                    firstArg<ByteBuffer>().put(data)
                    data.size
                } else {
                    0
                }

            fromTunnelCounter++
            dataSize
        }

        val dataSentToTunnel = mutableListOf<String>()
        var controlPacketCount = 0
        every { mockDatagramChannel.write(any<ByteBuffer>()) } answers {
            if (firstArg<ByteBuffer>().remaining() == 1) {
                controlPacketCount++
                1
            } else {
                val decoder = Charsets.UTF_8.newDecoder()
                dataSentToTunnel.add(decoder.decode(firstArg<ByteBuffer>()).toString())
                dataSentToTunnel.last().length
            }
        }

        var packetDataBroadcastCount = 0
        every { vpnService.sendBroadcast(any<Intent>()) } answers {
            val packetData =
                firstArg<Intent>().getParcelableArrayListExtraCompat<PacketData>("packetData")
            packetDataBroadcastCount += packetData?.size ?: 0
        }

        vpnConnected.set(vpnService, AtomicBoolean(true))
        vpnInterface.set(vpnService, mockk<ParcelFileDescriptor>())

        callForwardTraffic()

        val expectedDataSentToDevice =
            (0..<ToyVpnService.PACKET_DATA_BATCH_SIZE).map { "from tunnel $it" }
        assertEquals(expectedDataSentToDevice, dataSentToDevice)

        val expectedDataSentToTunnel =
            (0..<ToyVpnService.PACKET_DATA_BATCH_SIZE).map { "from device $it" }
        assertEquals(expectedDataSentToTunnel, dataSentToTunnel)

        assertEquals(ToyVpnService.SEND_CONTROL_PACKET_COUNT, controlPacketCount)
        assertEquals(
            expectedDataSentToTunnel.size + expectedDataSentToDevice.size,
            packetDataBroadcastCount,
        )
    }
}
