package com.pcapplusplus.toyvpn

import androidx.compose.material3.Text
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.Assert.assertEquals
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
    open fun setUp() {
        mockViewModel = mockk(relaxed = true)
    }

    fun renderScreen(
        vpnConnectionState: VpnConnectionState = VpnConnectionState.DISCONNECTED,
        vpnConnectionError: String? = null,
    ) {
        val vpnConnectionStateLiveData = MutableLiveData(vpnConnectionState)
        every { mockViewModel.vpnConnectionState } returns vpnConnectionStateLiveData
        val vpnConnectionErrorLiveData = MutableLiveData(vpnConnectionError)
        every { mockViewModel.vpnConnectionError } returns vpnConnectionErrorLiveData

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

        if (vpnConnectionState != VpnConnectionState.CONNECTED) {
            composeTestRule.onNodeWithTag("server_address_text_field").performTextClearance()
            composeTestRule.onNodeWithTag("server_port_text_field").performTextClearance()
            composeTestRule.onNodeWithTag("secret_text_field").performTextClearance()
        }

        composeTestRule.waitForIdle()
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
            renderScreen()

            composeTestRule.onNodeWithTag("server_address_text_field")
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
            renderScreen()

            composeTestRule.onNodeWithTag("server_port_text_field")
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

    class SharedPreferencesTests : BaseTest() {
        class MockSharedPreferencesProvider(private val preferences: MutableMap<String, String>) : SharedPreferencesProvider {
            override fun getSavedText(key: String): String {
                return preferences[key] ?: ""
            }

            override fun saveText(
                key: String,
                value: String,
            ) {
                preferences[key] = value
            }
        }

        private val serverAddress = "10.20.30.40"
        private val serverPort = "4321"
        private val secret = "secret"
        private val sharedPreferences =
            MockSharedPreferencesProvider(mutableMapOf("serverAddress" to serverAddress, "serverPort" to serverPort, "secret" to secret))

        @Before
        override fun setUp() {
            super.setUp()

            composeTestRule.setContent {
                ConnectScreen(navController = mockk(), viewModel = mockViewModel, sharedPreferencesProvider = sharedPreferences)
            }
        }

        @Test
        fun testConnectScreenLoadsSavedPreferences() {
            composeTestRule.onNodeWithTag("server_address_text_field")
                .assert(hasText(serverAddress))

            composeTestRule.onNodeWithTag("server_port_text_field")
                .assert(hasText(serverPort))

            composeTestRule.onNodeWithTag("secret_text_field")
                .assert(hasText(secret))
        }

        @Test
        fun testConnectScreenSavesPreferences() {
            composeTestRule.onNodeWithTag("server_address_text_field")
                .performTextClearance()
            composeTestRule.onNodeWithTag("server_address_text_field")
                .performTextInput("192.168.1.2")

            composeTestRule.onNodeWithTag("server_port_text_field")
                .performTextClearance()
            composeTestRule.onNodeWithTag("server_port_text_field")
                .performTextInput("9090")

            composeTestRule.onNodeWithTag("secret_text_field")
                .performTextClearance()
            composeTestRule.onNodeWithTag("secret_text_field")
                .performTextInput("newSecret")

            composeTestRule.onNodeWithText("Connect")
                .performClick()

            assertEquals("192.168.1.2", sharedPreferences.getSavedText("serverAddress"))
            assertEquals("9090", sharedPreferences.getSavedText("serverPort"))
            assertEquals("newSecret", sharedPreferences.getSavedText("secret"))
        }
    }

    class OtherTests : BaseTest() {
        @Test
        fun testElementsAreDisplayed() {
            renderScreen()

            composeTestRule.onNodeWithContentDescription("Logo").assertIsDisplayed()

            composeTestRule.onNodeWithTag("server_address_text_field").assertIsDisplayed()
            composeTestRule.onNodeWithTag("server_port_text_field").assertIsDisplayed()
            composeTestRule.onNodeWithTag("secret_text_field").assertIsDisplayed()
            composeTestRule.onNodeWithText("PcapPlusPlus Toy VPN").assertIsDisplayed()

            composeTestRule.onNodeWithText("Connect").assertIsDisplayed().assertIsEnabled()
        }

        @Test
        fun testSecretValidation() {
            renderScreen()

            composeTestRule.onNodeWithText("Connect").performClick()

            composeTestRule.onNodeWithText("Secret cannot be empty").assertIsDisplayed()
        }

        @Test
        fun testConnectButtonClick() {
            renderScreen()

            composeTestRule.onNodeWithTag("server_address_text_field").performTextInput("192.168.1.1")
            composeTestRule.onNodeWithTag("server_port_text_field").performTextInput("8080")
            composeTestRule.onNodeWithTag("secret_text_field").performTextInput("validSecret")

            composeTestRule.onNodeWithTag("server_address_error").assertDoesNotExist()
            composeTestRule.onNodeWithTag("server_port_error").assertDoesNotExist()
            composeTestRule.onNodeWithTag("secret_error").assertDoesNotExist()

            composeTestRule.onNodeWithText("Connect").performClick()

            verify { mockViewModel.connectVpn("192.168.1.1", 8080, "validSecret") }
        }

        @Test
        fun testVpnConnectionState_Connecting() {
            renderScreen(vpnConnectionState = VpnConnectionState.CONNECTING)

            composeTestRule.onNodeWithText("Connecting...")
                .assertIsDisplayed().assertIsNotEnabled()
        }

        @Test
        fun testVpnConnectionState_Connected() {
            renderScreen(vpnConnectionState = VpnConnectionState.CONNECTED)

            composeTestRule.onNodeWithText("Stats Screen")
                .assertIsDisplayed()
        }

        @Test
        fun testVpnConnectionError() {
            renderScreen(vpnConnectionError = "Some error occurred")

            composeTestRule.onNodeWithText("Some error occurred").assertExists()
        }
    }
}
