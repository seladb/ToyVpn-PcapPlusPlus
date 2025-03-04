package com.pcapplusplus.toyvpn

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pcapplusplus.toyvpn.model.VpnConnectionState
import java.util.regex.Pattern

val defaultSpacing = 16.dp
val defaultPadding = 16.dp

@Composable
fun ConnectScreen(
    navController: NavController,
    viewModel: ToyVpnViewModel,
) {
    val context = LocalContext.current

    var serverAddress by remember { mutableStateOf(getSavedText(context, "serverAddress")) }
    var serverPort by remember { mutableStateOf(getSavedText(context, "serverPort")) }
    var secret by remember { mutableStateOf(getSavedText(context, "secret")) }
    var serverAddressError by remember { mutableStateOf<String?>(null) }
    var serverPortError: String? by remember { mutableStateOf<String?>(null) }
    var secretError by remember { mutableStateOf<String?>(null) }

    val vpnConnectionState by viewModel.vpnConnectionState.observeAsState(VpnConnectionState.DISCONNECTED)
    val vpnConnectionError by viewModel.vpnConnectionError.observeAsState(null)

    fun validateIpv4Address(address: String): Boolean {
        val ipv4Pattern =
            Pattern.compile(
                "^((25[0-5]|2[0-4][0-9]|1[0-9]{1,2}|0?[1-9][0-9]{0,2}|)\\.)((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){2}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
            )
        return ipv4Pattern.matcher(address).matches()
    }

    fun validatePort(port: String): Boolean {
        return port.toIntOrNull()?.let {
            it in 1..65535
        } ?: false
    }

    val onConnectClicked: () -> Unit = {
        serverAddressError = null
        serverPortError = null
        secretError = null

        val validAddress = validateIpv4Address(serverAddress)
        val validPort = validatePort(serverPort)

        if (validAddress && validPort && secret.isNotEmpty()) {
            viewModel.connectVpn(
                serverAddress,
                serverPort.toIntOrNull() ?: 0,
                secret,
            )
            saveText(context, "serverAddress", serverAddress)
            saveText(context, "serverPort", serverPort)
            saveText(context, "secret", secret)
        } else {
            // If there's an issue with address or port
            serverAddressError =
                if (serverAddress.isEmpty()) {
                    "Server address cannot be empty"
                } else if (!validateIpv4Address(serverAddress)) {
                    "Invalid server address. Please enter a valid IPv4 address."
                } else {
                    null
                }

            serverPortError =
                if (serverPort.isEmpty()) {
                    "Port cannot be empty"
                } else if (!validatePort(serverPort)) {
                    "Invalid port number. Must be between 0 and 65535."
                } else {
                    null
                }

            secretError =
                if (secret.isEmpty()) {
                    "Secret cannot be empty"
                } else {
                    null
                }
        }
    }

    LaunchedEffect(vpnConnectionState) {
        if (vpnConnectionState == VpnConnectionState.CONNECTED) {
            navController.navigate("stats_screen")
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(defaultPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 32.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.pcapplusplus_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PcapPlusPlus Toy VPN",
                fontSize = 32.sp,
                color = Color.Black,
            )
        }

        OutlinedTextField(
            value = serverAddress,
            onValueChange = { serverAddress = it },
            label = { Text(text = "Server Address") },
            modifier = Modifier.fillMaxWidth().testTag("server_address_text_field"),
            leadingIcon = { Icon(imageVector = Icons.Default.Language, null) },
            shape = RoundedCornerShape(16.dp),
            isError = serverAddressError != null,
        )

        serverAddressError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp).testTag("server_address_error"),
            )
        }

        Spacer(modifier = Modifier.height(defaultSpacing))

        OutlinedTextField(
            value = serverPort,
            onValueChange = { serverPort = it },
            label = { Text(text = "Server Port") },
            modifier = Modifier.fillMaxWidth().testTag("server_port_text_field"),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(imageVector = Icons.Default.Numbers, null) },
            shape = RoundedCornerShape(16.dp),
            isError = serverPortError != null,
        )

        serverPortError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp).testTag("server_port_error"),
            )
        }

        Spacer(modifier = Modifier.height(defaultSpacing))

        OutlinedTextField(
            value = secret,
            onValueChange = { secret = it },
            label = { Text(text = "Secret") },
            modifier = Modifier.fillMaxWidth().testTag("secret_text_field"),
            leadingIcon = { Icon(imageVector = Icons.Default.Key, null) },
            shape = RoundedCornerShape(16.dp),
            isError = secretError != null,
        )

        secretError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp).testTag("secret_error"),
            )
        }

        Spacer(modifier = Modifier.height(defaultSpacing))

        Button(
            onClick = onConnectClicked,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            enabled = vpnConnectionState != VpnConnectionState.CONNECTING,
        ) {
            if (vpnConnectionState == VpnConnectionState.CONNECTING) {
                Text("Connecting...")
            } else {
                Text("Connect")
            }
        }

        vpnConnectionError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

fun getSavedText(
    context: Context,
    key: String,
): String {
    val sharedPreferences = context.getSharedPreferences("ToyVpnPreferences", Context.MODE_PRIVATE)
    return sharedPreferences.getString(key, "") ?: ""
}

fun saveText(
    context: Context,
    key: String,
    value: String,
) {
    val sharedPreferences = context.getSharedPreferences("ToyVpnPreferences", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString(key, value)
    editor.apply()
}

@Preview(showBackground = true)
@Composable
fun PreviewConnectScreen() {
    ConnectScreen(navController = rememberNavController(), viewModel = viewModel())
}
