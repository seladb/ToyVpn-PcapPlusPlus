package com.pcapplusplus.toyvpn

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pcapplusplus.toyvpn.model.VpnSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.InvocationTargetException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

@RunWith(AndroidJUnit4::class)
class ToyVpnServiceHandshakeTest {
    private val vpnService = ToyVpnService()
    private val handshakeMethod =
        vpnService::class.java.getDeclaredMethod(
            "handshake",
            String::class.java,
            Int::class.java,
            String::class.java,
            Boolean::class.java,
        ).apply {
            isAccessible = true
        }
    private val handshakeSecret = "secret"
    private lateinit var mockedDatagramChannel: DatagramChannel

    private fun callHandshake(
        serverAddress: String = "1.2.3.4",
        serverPort: Int = 8000,
        secret: String = handshakeSecret,
        protectTunnel: Boolean = false,
    ): Pair<DatagramChannel, VpnSettings> {
        val parameters = arrayOf<Any>(serverAddress, serverPort, secret, protectTunnel)
        try {
            return handshakeMethod.invoke(
                vpnService,
                *parameters,
            ) as Pair<DatagramChannel, VpnSettings>
        } catch (ex: InvocationTargetException) {
            throw ex.cause!!
        }
    }

    @Before
    fun setup() {
        mockkStatic(DatagramChannel::class)
        mockedDatagramChannel = mockk<DatagramChannel>(relaxed = true)
        every { DatagramChannel.open() } returns mockedDatagramChannel
    }

    @Test
    fun testSuccessfulHandshake() {
        every { mockedDatagramChannel.write(any<ByteBuffer>()) } answers {
            val bytes = ByteArray(firstArg<ByteBuffer>().remaining())
            firstArg<ByteBuffer>().get(bytes)
            assertEquals(0.toByte(), bytes[0])
            assertEquals("very-secret", String(bytes.sliceArray(1 until bytes.size)))
            bytes.size
        }
        every { mockedDatagramChannel.read(any<ByteBuffer>()) } answers {
            val responseAsByteArray = byteArrayOf(0) + "a,10.0.0.1,24".encodeToByteArray()
            firstArg<ByteBuffer>().put(responseAsByteArray).flip()
            responseAsByteArray.size
        }

        val result = callHandshake("11.22.33.44", 5432, "very-secret")

        verify { mockedDatagramChannel.connect(InetSocketAddress("11.22.33.44", 5432)) }
        verify { mockedDatagramChannel.configureBlocking(false) }
        assertEquals(mockedDatagramChannel, result.first)
        assertEquals("10.0.0.1", result.second.clientAddress)
    }

    @Test
    fun testCannotProtectTunnel() {
        val ex =
            assertThrows(IllegalStateException::class.java) {
                callHandshake(protectTunnel = true)
            }

        assertEquals("Cannot protect the tunnel", ex.message)
    }

    @Test
    fun testFailWritingToTunnel() {
        every { mockedDatagramChannel.write(any<ByteBuffer>()) } returns 0

        val exception =
            assertThrows(IllegalStateException::class.java) {
                callHandshake()
            }
        assertEquals("Failed to send control packet to tunnel", exception.message)
    }

    @Test
    fun testTunnelDoesNotRespond() {
        every { mockedDatagramChannel.write(any<ByteBuffer>()) } returns handshakeSecret.length + 1

        val exception =
            assertThrows(IllegalStateException::class.java) {
                callHandshake()
            }
        assertEquals("Cannot complete handshake with the server", exception.message)
    }

    @Test
    fun testBadResponseFromTunnel() {
        every { mockedDatagramChannel.write(any<ByteBuffer>()) } returns handshakeSecret.length + 1
        every { mockedDatagramChannel.read(any<ByteBuffer>()) } answers {
            firstArg<ByteBuffer>().put(0)
            firstArg<ByteBuffer>().put("invalid".toByteArray())
            5
        }

        val exception =
            assertThrows(IllegalStateException::class.java) {
                callHandshake()
            }
        assertEquals("Cannot parse the handshake response from the server", exception.message)
    }

    @Test
    fun testTunnelResponseDoesNotStartWithZero() {
        every { mockedDatagramChannel.write(any<ByteBuffer>()) } returns handshakeSecret.length + 1
        every { mockedDatagramChannel.read(any<ByteBuffer>()) } answers {
            firstArg<ByteBuffer>().put("a,10.0.0.1,24".toByteArray())
            12
        }

        val exception =
            assertThrows(IllegalStateException::class.java) {
                callHandshake()
            }
        assertEquals("Cannot complete handshake with the server", exception.message)
    }
}
