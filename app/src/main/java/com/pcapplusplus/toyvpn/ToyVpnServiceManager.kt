package com.pcapplusplus.toyvpn

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.pcapplusplus.toyvpn.model.BroadcastActions
import com.pcapplusplus.toyvpn.model.PacketData
import com.pcapplusplus.toyvpn.model.VpnConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface PacketDataHandler {
    fun onPacketDataArrives(packetDataList: ArrayList<PacketData>)
}

interface VpnServiceProxy {
    fun startVpnService(intent: Intent)

    fun prepare(context: Context): Intent?
}

open class DefaultVpnServiceProxy(private val context: Context) : VpnServiceProxy {
    override fun startVpnService(intent: Intent) {
        context.startService(intent)
    }

    override fun prepare(context: Context): Intent? {
        return VpnService.prepare(context)
    }
}

class ToyVpnServiceManager(
    private val context: Context,
    vpnPrepareActivityResultLauncher: ActivityResultLauncher<Intent>,
    private val vpnServiceProxy: VpnServiceProxy = DefaultVpnServiceProxy(context),
) {
    private val _vpnServiceState = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    private val _clientAddress = MutableStateFlow<String?>(null)
    private val _vpnConnectionError = MutableStateFlow<String?>(null)
    private val packetDataHandlers: MutableList<PacketDataHandler> = mutableListOf()

    val vpnServiceState: StateFlow<VpnConnectionState> get() = _vpnServiceState
    val clientAddress: StateFlow<String?> get() = _clientAddress
    val vpnConnectionError: StateFlow<String?> get() = _vpnConnectionError

    private val vpnStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    BroadcastActions.VPN_SERVICE_STARTED -> {
                        Log.e("TestReceiveVpnStartedAndStoppedEvent", "Got VPN_SERVICE_STARTED event")
                        _vpnServiceState.value = VpnConnectionState.CONNECTED
                        _clientAddress.value = intent.getStringExtra("clientAddress")
                        Log.e("TestReceiveVpnStartedAndStoppedEvent", "VPN_SERVICE_STARTED: _clientAddress.value = ${_clientAddress.value}")
                        Log.e(
                            "TestReceiveVpnStartedAndStoppedEvent",
                            "VPN_SERVICE_STARTED: _vpnServiceState.value = ${_vpnServiceState.value}",
                        )
                    }

                    BroadcastActions.VPN_SERVICE_STOPPED -> {
                        Log.e("TestReceiveVpnStartedAndStoppedEvent", "Got VPN_SERVICE_STOPPED event")
                        _vpnServiceState.value = VpnConnectionState.DISCONNECTED
                        _clientAddress.value = null
                        Log.e(
                            "TestReceiveVpnStartedAndStoppedEvent",
                            "VPN_SERVICE_STOPPED: _vpnServiceState.value = ${_vpnServiceState.value}",
                        )
                    }

                    BroadcastActions.VPN_SERVICE_PACKET_ARRIVED -> {
                        if (packetDataHandlers.size > 0) {
                            val packetDataList =
                                intent.getParcelableArrayListExtraCompat<PacketData>("packetData")
                            if (packetDataList != null) {
                                for (packetDataHandler in packetDataHandlers) {
                                    packetDataHandler.onPacketDataArrives(packetDataList)
                                }
                            }
                        }
                    }

                    BroadcastActions.VPN_SERVICE_ERROR -> {
                        _vpnServiceState.value = VpnConnectionState.DISCONNECTED
                        _clientAddress.value = null
                        _vpnConnectionError.value = intent.getStringExtra("errorMessage")
                    }
                }
            }
        }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver() {
        val filter =
            IntentFilter().apply {
                addAction(BroadcastActions.VPN_SERVICE_STARTED)
                addAction(BroadcastActions.VPN_SERVICE_STOPPED)
                addAction(BroadcastActions.VPN_SERVICE_PACKET_ARRIVED)
                addAction(BroadcastActions.VPN_SERVICE_ERROR)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(vpnStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(vpnStateReceiver, filter)
        }
    }

    init {
        val prepareIntent = vpnServiceProxy.prepare(context)
        if (prepareIntent != null) {
            vpnPrepareActivityResultLauncher.launch(prepareIntent)
        }

        registerReceiver()
    }

    fun startVpnService(
        serverAddress: String,
        serverPort: Int,
        secret: String,
    ) {
        _vpnServiceState.value = VpnConnectionState.CONNECTING
        _vpnConnectionError.value = null

        val intent =
            Intent(context, ToyVpnService::class.java).apply {
                putExtra("serverAddress", serverAddress)
                putExtra("serverPort", serverPort)
                putExtra("serverSecret", secret)
            }

        vpnServiceProxy.startVpnService(intent)
    }

    fun stopVpnService() {
        _vpnServiceState.value = VpnConnectionState.DISCONNECTING
        _vpnConnectionError.value = null

        context.sendBroadcast(Intent(BroadcastActions.VPN_SERVICE_STOP))
    }

    fun registerPacketDataHandler(handler: PacketDataHandler) {
        packetDataHandlers.add(handler)
    }

    fun close() {
        context.unregisterReceiver(vpnStateReceiver)
        packetDataHandlers.clear()
    }
}
