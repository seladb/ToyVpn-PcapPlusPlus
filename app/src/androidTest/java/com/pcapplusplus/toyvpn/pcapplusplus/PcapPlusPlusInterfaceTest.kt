package com.pcapplusplus.toyvpn.pcapplusplus

import com.pcapplusplus.toyvpn.hexToByteArray
import com.pcapplusplus.toyvpn.model.PacketData
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.Serializable

@RunWith(Parameterized::class)
class PcapPlusPlusInterfaceTest(
    private val rawPacket: ByteArray,
    private val expectedJson: String,
) {
    private val pcapPlusPlusInterface = PcapPlusPlusInterface()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "test raw packet data, expected json is {1}")
        fun getPacketAnalysisResults(): List<Array<Serializable>> {
            return listOf(
                arrayOf(
                    "45c0001cf77000000102d6b50a3c00bde00000011164ee9b00000000000000000000000000000000000000000000".hexToByteArray(),
                    Json.encodeToString(PacketData(isIPv4 = true, length = 46)),
                ),
                arrayOf(
                    "6000000000202b04220000000000024402123ffffeae22f7220000000000024000020000000000043a02000100000000220000000000021000020000000000048000d3ab00000000".hexToByteArray(),
                    Json.encodeToString(PacketData(isIPv6 = true, length = 72)),
                ),
                arrayOf(
                    "45c0002800080000ff063987c0a80021c0a8000f00b3084b7553d58cd20bb75650193ee5122500000101080a0ad4".hexToByteArray(),
                    Json.encodeToString(PacketData(isIPv4 = true, isTCP = true, connectionID = 1917828495L, length = 46)),
                ),
                arrayOf(
                    "450000c840d040004011e1320a00020f0a0002144440177000b418e880098d5600000280043daabafeddf4de73b77bff7df4dcf19a92674c7596b455f95c9adef85974dbf3def6fbbc787cf9b4bf76fcfc739a79397c738a2b941abadfde5cfddadbf55f5f76fbdab7fd7a7b35d5ea96335f7fef8e6ad8fdf819bc7d5efd5b7dd6befcd5ff75d5bbf9fe5c715dbd3272926dfdbead17b3be334fe95fd3ef997ad6fd5dd9fad9567395ddf7d97976fc79ff759f2fbe7e2cd6b4f134bbdc70cdf27c54f6da127e59d8".hexToByteArray(),
                    Json.encodeToString(PacketData(isIPv4 = true, isUDP = true, connectionID = 61303140L, length = 200)),
                ),
                arrayOf(
                    "4500004f000000009b111120ca669803ac1000040035b7f0003b7235d4678180000100010000000009616e616c79746963730331363303636f6d0000010001c00c00010001000003710004dffcc385".hexToByteArray(),
                    Json.encodeToString(PacketData(isIPv4 = true, isUDP = true, connectionID = 474174079L, isDNS = true, length = 79)),
                ),
                arrayOf(
                    "45000041171f000039115c0fac100004ca669803b48a0035002d7fe52fcf01000001000000000000077a313633706963017606627367736c6202636e0000010001".hexToByteArray(),
                    Json.encodeToString(
                        PacketData(
                            isIPv4 = true,
                            isUDP = true,
                            connectionID = 3124694454L,
                            isDNS = true,
                            dnsQuery = "z163pic.v.bsgslb.cn",
                            length = 65,
                        ),
                    ),
                ),
                arrayOf(
                    "450000574a944000360600003ff5d15bc0a8010201bb04ddf41722c55b86b1f850181d50dad0000014030100010116030100244fd8a7e2a579aebd36aa0fdfe696838a81ba5e271c979b52129fff7a1955356d93d1fd51".hexToByteArray(),
                    Json.encodeToString(PacketData(isIPv4 = true, isTCP = true, connectionID = 3766857573L, isTLS = true, length = 87)),
                ),
                arrayOf(
                    "450000d10a72400080061cbac0a801023ff5d15b04dd01bb5b86b015f41711625018ffffba6f000016030100a4010000a003014afd973370b2880c4727492b27cea80b839822004fbfc1512411edcb543c9853000046c00ac0140088008700390038c00fc00500840035c007c009c011c0130045004400330032c00cc00ec002c0040096004100040005002fc008c01200160013c00dc003feff000a010000310000001700150000126164646f6e732e6d6f7a696c6c612e6f7267000a00080006001700180019000b000201000023000050b93a92".hexToByteArray(),
                    Json.encodeToString(
                        PacketData(
                            isIPv4 = true,
                            isTCP = true,
                            connectionID = 3766857573L,
                            isTLS = true,
                            tlsServerName = "addons.mozilla.org",
                            length = 213,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun testAnalyzePacket() {
        assertEquals(expectedJson, String(pcapPlusPlusInterface.analyzePacket(rawPacket)))
    }
}
