import com.pcapplusplus.toyvpn.model.VpnSettings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class VpnSettingsTest {
    @Test
    fun `test valid parameters for vpn settings`() {
        val paramString = "a,192.168.1.1,24 r,10.0.0.1,16 m,1500 d,8.8.8.8"
        val vpnSettings = VpnSettings.fromParamString(paramString)

        assertEquals("192.168.1.1", vpnSettings.clientAddress)
        assertEquals(24, vpnSettings.clientAddressPrefixLength)
        assertEquals("10.0.0.1", vpnSettings.routeAddress)
        assertEquals(16, vpnSettings.routePrefixLength)
        assertEquals(1500, vpnSettings.mtu)
        assertEquals("8.8.8.8", vpnSettings.dnsServer)
    }

    @Test
    fun `test missing optional dns server`() {
        val paramString = "a,192.168.1.1,24 r,10.0.0.1,16 m,1400"
        val vpnSettings = VpnSettings.fromParamString(paramString)

        // Check that the DNS server is null (optional field)
        assertNull(vpnSettings.dnsServer)
    }

    @Test
    fun `test default values for missing parameters`() {
        val paramString = "a,192.168.1.1,24"
        val vpnSettings = VpnSettings.fromParamString(paramString)

        // Check that defaults are used correctly for missing parameters
        assertEquals("192.168.1.1", vpnSettings.clientAddress)
        assertEquals(24, vpnSettings.clientAddressPrefixLength)
        assertEquals("0.0.0.0", vpnSettings.routeAddress) // default route address
        assertEquals(0, vpnSettings.routePrefixLength) // default route prefix length
        assertEquals(1400, vpnSettings.mtu) // default mtu
        assertNull(vpnSettings.dnsServer) // dns server should be null
    }

    @Test
    fun `test invalid parameter format throws IllegalArgumentException`() {
        val invalidParamString = "a,192.168.1.1,not_a_number"

        // The exception should be thrown for invalid format
        val exception = assertThrows(IllegalArgumentException::class.java) {
            VpnSettings.fromParamString(invalidParamString)
        }
        assertTrue(exception.message?.contains("Bad parameter") == true)
    }

    @Test
    fun `test missing required parameters throws IllegalArgumentException`() {
        val paramString = "a,192.168.1.1 r,10.0.0.1,16"

        // The exception should be thrown for missing client address prefix length
        val exception = assertThrows(IllegalArgumentException::class.java) {
            VpnSettings.fromParamString(paramString)
        }
        assertTrue(exception.message?.contains("Bad parameter") == true)
    }

    @Test
    fun `test valid parameters with no route or mtu`() {
        val paramString = "a,192.168.1.1,24"
        val vpnSettings = VpnSettings.fromParamString(paramString)

        // Check that the routeAddress and mtu are using default values
        assertEquals("0.0.0.0", vpnSettings.routeAddress)
        assertEquals(0, vpnSettings.routePrefixLength)
        assertEquals(1400, vpnSettings.mtu)
    }
}
