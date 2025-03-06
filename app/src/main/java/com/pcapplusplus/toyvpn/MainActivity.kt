package com.pcapplusplus.toyvpn

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pcapplusplus.toyvpn.ui.theme.ToyVpnPcapPlusPlusTheme

class MainActivity : ComponentActivity() {
    private lateinit var toyVpnServiceManager: ToyVpnServiceManager
    private lateinit var toyVpnViewModel: ToyVpnViewModel

    private val vpnPrepareResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                Log.e("ToyVpnMainActivity", "VPN permissions not granted")
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        toyVpnServiceManager =
            ToyVpnServiceManager(application.baseContext, vpnPrepareResultLauncher)
        toyVpnViewModel = ToyVpnViewModel(toyVpnServiceManager)

        setContent {
            ToyVpnPcapPlusPlusTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "connect_screen",
                ) {
                    composable("connect_screen") {
                        ConnectScreen(
                            navController,
                            toyVpnViewModel,
                        )
                    }
                    composable("stats_screen") {
                        StatsScreen(navController, toyVpnViewModel)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toyVpnServiceManager.close()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainActivity() {
    ConnectScreen(navController = rememberNavController(), viewModel = viewModel())
}
