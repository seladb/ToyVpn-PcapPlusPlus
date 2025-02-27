package com.pcapplusplus.toyvpn

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.MutableLiveData
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pcapplusplus.toyvpn.model.VpnConnectionState
import com.pcapplusplus.toyvpn.ui.theme.ToyVpnPcapPlusPlusTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

open class BaseTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    protected lateinit var mockViewModel: ToyVpnViewModel

    @Before
    fun setUp() {
        mockViewModel = mockk(relaxed = true)

        composeTestRule.setContent {
            ToyVpnPcapPlusPlusTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "connect_screen",
                ) {
                    composable("connect_screen") {
                        ConnectScreen(
                            navController,
                            mockViewModel,
                        )
                    }
                    composable("stats_screen") {
                        Text("Stats Screen")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Server Address").performTextClearance()
        composeTestRule.onNodeWithText("Server Port").performTextClearance()
        composeTestRule.onNodeWithText("Secret").performTextClearance()
    }
}

@RunWith(Enclosed::class)
class ConnectScreenTest {
    @RunWith(Parameterized::class)
    class ServerAddressValidationTest(private val inputValue: String, private val isValid: Boolean) : BaseTest() {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: testServerAddressValidation({0}) = {1}")
            fun serverAddressData(): Collection<Array<Any>> {
                return listOf(
                    arrayOf("178.27.12.113", true),
                    arrayOf("invalid_address", false),
                    arrayOf("256.256.256.256", false),
                    arrayOf("192.168.0.1", true),
                    arrayOf("10.0.0.1", true),
                    arrayOf("0.0.0.0", false),
                    arrayOf("10.0.0.abc", false),
                    arrayOf("1.127", false),
                    arrayOf("10.-1.0.4", false),
                )
            }
        }

        @Test
        fun testServerAddressValidation() {
            composeTestRule.onNodeWithText("Server Address")
                .performTextInput(inputValue)

            composeTestRule.onNodeWithText("Connect").performClick()

            if (isValid) {
                composeTestRule.onNodeWithText("Invalid server address. Please enter a valid IPv4 address.")
                    .assertDoesNotExist()
            } else {
                composeTestRule.onNodeWithText("Invalid server address. Please enter a valid IPv4 address.")
                    .assertIsDisplayed()
            }
        }
    }

    @RunWith(Parameterized::class)
    class ServerPortValidationTest(private val inputValue: String, private val isValid: Boolean) : BaseTest() {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: testServerPortValidation({0}) = {1}")
            fun serverPortData(): Collection<Array<Any>> {
                return listOf(
                    arrayOf("8080", true),
                    arrayOf("70000", false),
                    arrayOf("-1", false),
                    arrayOf("65535", true),
                    arrayOf("0", false),
                    arrayOf("invalid", false),
                )
            }
        }

        @Test
        fun testServerPortValidation() {
            composeTestRule.onNodeWithText("Server Port")
                .performTextInput(inputValue)

            composeTestRule.onNodeWithText("Connect").performClick()

            if (isValid) {
                composeTestRule.onNodeWithText("Invalid port number. Must be between 0 and 65535.")
                    .assertDoesNotExist()
            } else {
                composeTestRule.onNodeWithText("Invalid port number. Must be between 0 and 65535.")
                    .assertIsDisplayed()
            }
        }
    }

    class OtherTests : BaseTest() {
        @Test
        fun testElementsAreDisplayed() {
            composeTestRule.onNodeWithContentDescription("Logo").assertIsDisplayed()

            composeTestRule.onNodeWithText("Server Address").assertIsDisplayed()
            composeTestRule.onNodeWithText("Server Port").assertIsDisplayed()
            composeTestRule.onNodeWithText("Secret").assertIsDisplayed()
            composeTestRule.onNodeWithText("PcapPlusPlus Toy VPN").assertIsDisplayed()

            composeTestRule.onNodeWithText("Connect").assertIsDisplayed().assertIsEnabled()
        }

        @Test
        fun testSecretValidation() {
            composeTestRule.onNodeWithText("Connect").performClick()

            composeTestRule.onNodeWithText("Secret cannot be empty").assertIsDisplayed()
        }

        @Test
        fun testConnectButtonClick() {
            composeTestRule.onNodeWithText("Server Address").performTextInput("192.168.1.1")
            composeTestRule.onNodeWithText("Server Port").performTextInput("8080")
            composeTestRule.onNodeWithText("Secret").performTextInput("validSecret")

            composeTestRule.onNodeWithText("Connect").performClick()

            verify { mockViewModel.connectVpn("192.168.1.1", 8080, "validSecret") }
        }

        @Test
        fun testVpnConnectionState_Connecting() {
            val vpnConnectionStateLiveData = MutableLiveData(VpnConnectionState.CONNECTING)
            every { mockViewModel.vpnConnectionState } returns vpnConnectionStateLiveData

            composeTestRule.onNodeWithText("Connecting...")
                .assertIsDisplayed().assertIsNotEnabled()
        }

        @Test
        fun testVpnConnectionState_Connected() {
            val vpnConnectionStateLiveData = MutableLiveData(VpnConnectionState.CONNECTED)
            every { mockViewModel.vpnConnectionState } returns vpnConnectionStateLiveData

            composeTestRule.onNodeWithText("Stats Screen")
                .assertIsDisplayed()
        }

        @Test
        fun testVpnConnectionError() {
            val vpnConnectionErrorLiveData = MutableLiveData("Some error occurred")
            every { mockViewModel.vpnConnectionError } returns vpnConnectionErrorLiveData

            composeTestRule.onNodeWithText("Some error occurred")
                .assertIsDisplayed()
        }
    }
}
