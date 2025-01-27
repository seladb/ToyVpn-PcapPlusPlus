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
    //    private val toyVpnServiceManager = ToyVpnServiceManager(application.baseContext)
//    private val toyVpnViewModel: ToyVpnViewModel by viewModels {
//        ToyVpnViewModel.provideFactory(toyVpnServiceManager)
//    }
    private lateinit var toyVpnServiceManager: ToyVpnServiceManager
    private lateinit var toyVpnViewModel: ToyVpnViewModel

    // Create an ActivityResultLauncher for the result from the VPN service prepare activity
    private val vpnPrepareResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                // Handle the case where the user denied the permission
                Log.e("MainActivity", "VPN permission not granted")
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
                // Set up the NavController and NavHost
                val navController = rememberNavController()

                // Define the NavHost with two screens: ConnectScreen and LogScreen
                NavHost(
                    navController = navController,
                    startDestination = "connect_screen" // Starting point
                ) {
                    composable("connect_screen") {
                        ConnectScreen(
                            navController,
                            toyVpnViewModel
                        ) // Passing navController to ConnectScreen
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
