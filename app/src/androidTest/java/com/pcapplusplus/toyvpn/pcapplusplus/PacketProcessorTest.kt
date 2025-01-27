package com.pcapplusplus.toyvpn.pcapplusplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pcapplusplus.toyvpn.model.PacketData
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PacketProcessorTest {
    private val mockPcapPlusPlusInterface = mockk<PcapPlusPlusInterface>(relaxed = true)
    private val packetProcessor = PacketProcessor(mockPcapPlusPlusInterface)
    private val analyzePacketInput = byteArrayOf(1)

    @Test
    fun testValidPayload() {
        every { mockPcapPlusPlusInterface.analyzePacket(any()) } returns "{\"isIPv4\": true, \"length\": 10}".toByteArray()
        assertEquals(
            PacketData(isIPv4 = true, length = 10),
            packetProcessor.processPacket(analyzePacketInput)
        )
    }

    @Test
    fun testInvalidPayload() {
        every { mockPcapPlusPlusInterface.analyzePacket(any()) } returns "invalid".toByteArray()
        assertNull(packetProcessor.processPacket(analyzePacketInput))
    }
}