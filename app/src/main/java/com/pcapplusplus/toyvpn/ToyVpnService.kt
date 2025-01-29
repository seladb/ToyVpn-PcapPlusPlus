package com.pcapplusplus.toyvpn

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.pcapplusplus.toyvpn.model.BroadcastActions
import com.pcapplusplus.toyvpn.model.PacketData
import com.pcapplusplus.toyvpn.model.VpnSettings
import com.pcapplusplus.toyvpn.pcapplusplus.PacketProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.charset.StandardCharsets.US_ASCII
import java.util.concurrent.atomic.AtomicBoolean

class ToyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnConnected: AtomicBoolean = AtomicBoolean(false)
    private val packetProcessor = PacketProcessor()
    private val packetDataList: MutableList<PacketData> = mutableListOf()
    private var lastPacketDataSentTimestamp: Long = 0

    private val vpnServiceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val LOG_TAG = "ToyVpnService"
        const val PACKET_DATA_BATCH_SIZE = 20
        const val PACKET_DATA_BATCH_INTERVAL_MSEC = 1000
        const val IDLE_INTERVAL_MSEC: Long = 100
        const val KEEPALIVE_INTERVAL_MSEC: Long = 15
        const val MAX_HANDSHAKE_ATTEMPTS = 50
        const val MAX_PACKET_SIZE = 32767
        const val MAX_SECRET_LENGTH = 1024
    }

    @SuppressLint("SyntheticAccessor")
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BroadcastActions.VPN_SERVICE_STOP) {
                Log.w(LOG_TAG, "Got ${BroadcastActions.VPN_SERVICE_STOP}, stopping self")
                stopSelf()
                disconnectVpn()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                broadcastReceiver,
                IntentFilter(BroadcastActions.VPN_SERVICE_STOP),
                Context.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(broadcastReceiver, IntentFilter(BroadcastActions.VPN_SERVICE_STOP))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val serverAddress = requireNotNull(intent?.getStringExtra("serverAddress"))
            val serverPort = requireNotNull(intent?.getIntExtra("serverPort", 0))
            val serverSecret = requireNotNull(intent?.getStringExtra("serverSecret"))

            vpnServiceScope.launch {
                val establishVpnResult = async {
                    try {
                        establishVpnConnection(serverAddress, serverPort, serverSecret)
                    } catch (e: Exception) {
                        stopSelfOnError("Error establishing the VPN connection", e)
                        null
                    }
                }

                val vpnTunnel = establishVpnResult.await() ?: return@launch

                try {
                    forwardTraffic(vpnInterface?.fileDescriptor, vpnTunnel)
                } catch (e: Exception) {
                    stopSelfOnError("Error while forwarding traffic to the VPN server", e)
                }
            }
        } catch (e: IllegalArgumentException) {
            stopSelfOnError("Internal error", e)
        }

        return START_STICKY
    }

    private fun establishVpnConnection(
        serverAddress: String,
        serverPort: Int,
        secret: String
    ): DatagramChannel {
        val handshakeResponse = handshake(serverAddress, serverPort, secret)
        val vpnSettings = handshakeResponse.second
        Log.w(LOG_TAG, "VPN settings: $vpnSettings")

        val builder = Builder()

        builder.setSession("ToyVPN")
        builder.addAddress(vpnSettings.clientAddress, vpnSettings.clientAddressPrefixLength)
        builder.setMtu(vpnSettings.mtu)
        builder.addRoute(vpnSettings.routeAddress, vpnSettings.routePrefixLength)

        if (vpnSettings.dnsServer != null) {
            builder.addDnsServer(vpnSettings.dnsServer)
        }

        vpnInterface = builder.establish()
        vpnConnected.set(true)

        Log.w(
            LOG_TAG,
            "vpnInterface: $vpnInterface, ${vpnInterface?.fileDescriptor}"
        )
        sendBroadcast(Intent(BroadcastActions.VPN_SERVICE_STARTED))
        Log.w(LOG_TAG, "broadcast ${BroadcastActions.VPN_SERVICE_STARTED}")
        return handshakeResponse.first
    }

    private fun forwardTraffic(vpnInterfaceDescriptor: FileDescriptor?, tunnel: DatagramChannel) {
        val inputStream = FileInputStream(vpnInterfaceDescriptor)
        val outputStream = FileOutputStream(vpnInterfaceDescriptor)
        val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)

        var lastSendTime = System.currentTimeMillis()

        while (vpnInterface != null && vpnConnected.get()) {
            var idle = true

            var length = inputStream.read(packet.array())
            if (length > 0) {
//                    Log.w(LOG_TAG, "Captured packet from device of length: $length")
                packet.limit(length)
                processPacket(packet.array())
                tunnel.write(packet)

                idle = false
            }

            packet.clear()

            length = tunnel.read(packet)
            if (length > 0) {
                packet.limit(length)
                packet.flip()
                if (packet.get(0).toInt() != 0) {
//                        Log.w(LOG_TAG, "Captured packet from tunnel of length: $length")
                    processPacket(packet.array())
                    outputStream.write(packet.array(), 0, length)
                }

                idle = false
            }

            packet.clear()

            if (idle) {
                Thread.sleep(IDLE_INTERVAL_MSEC)

                val timeNow = System.currentTimeMillis()

                if (lastSendTime + KEEPALIVE_INTERVAL_MSEC <= timeNow) {
                    // We are receiving for a long time but not sending.
                    // Send empty control messages.
                    packet.put(0.toByte()).limit(1)
                    for (i in 0..2) {
                        packet.position(0)
                        tunnel.write(packet)
                    }
                    packet.clear()
                    lastSendTime = timeNow
                }
            }
        }
    }

    private fun processPacket(rawPacketData: ByteArray) {
        val packetData = packetProcessor.processPacket(rawPacketData = rawPacketData)
        if (packetData != null) {
            val curTimestamp = System.currentTimeMillis()
            packetDataList.add(packetData)
            if (packetDataList.size >= PACKET_DATA_BATCH_SIZE || curTimestamp - lastPacketDataSentTimestamp > PACKET_DATA_BATCH_INTERVAL_MSEC) {
                val intent = Intent(BroadcastActions.VPN_SERVICE_PACKET_ARRIVED).apply {
                    putParcelableArrayListExtra("packetData", ArrayList(packetDataList))
                }
                Log.w(LOG_TAG, "sending ${packetDataList.size} packet data")
                sendBroadcast(intent)
                packetDataList.clear()
                lastPacketDataSentTimestamp = curTimestamp
            }
        }
    }

    private fun handshake(
        serverAddress: String,
        serverPort: Int,
        secret: String,
        protectTunnel: Boolean = true
    ): Pair<DatagramChannel, VpnSettings> {
        Log.w(LOG_TAG, "Starting handshake")

        if (secret.length > MAX_SECRET_LENGTH) {
            throw IllegalArgumentException("Secret is too long, max allowed length is $MAX_SECRET_LENGTH")
        }

        val tunnel = DatagramChannel.open()
        if (protectTunnel && !protect(tunnel.socket())) {
            throw IllegalStateException("Cannot protect the tunnel")
        }
        Log.w(LOG_TAG, "Opened channel")
        val socketAddress = InetSocketAddress(serverAddress, serverPort)
        tunnel.connect(socketAddress)
        tunnel.configureBlocking(false)
        Log.w(LOG_TAG, "Channel connected")

        val packet = ByteBuffer.allocate(MAX_SECRET_LENGTH + 1)
        val secretAsByteArray = byteArrayOf(0) + secret.encodeToByteArray()
        packet.put(secretAsByteArray).flip()

        if (tunnel.write(packet) != secretAsByteArray.size) {
            throw IllegalStateException("Failed to send control packet to tunnel")
        }
        Log.w(LOG_TAG, "Sent secret")

        packet.clear()

        // Wait for the parameters within a limited time.
        for (i in 0..MAX_HANDSHAKE_ATTEMPTS) {
            Thread.sleep(IDLE_INTERVAL_MSEC)

            // Normally we should not receive random packets. Check that the first
            // byte is 0 as expected.
            val length = tunnel.read(packet)
            if (length > 0 && packet[0].toInt() == 0) {
                try {
                    val response =
                        String(packet.array(), 1, length - 1, US_ASCII).trim { it <= ' ' }
                    Log.w(LOG_TAG, "Got handshake response: $response")
                    return Pair(tunnel, VpnSettings.fromParamString(response))
                } catch (ex: Exception) {
                    throw IllegalStateException("Cannot parse the handshake response from the server")
                }
            }
        }

        throw IllegalStateException("Cannot complete handshake with the server")
    }

    private fun disconnectVpn() {
        try {
            vpnConnected.set(false)
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopSelfOnError(errorMessage: String, exception: java.lang.Exception) {
        Log.e(LOG_TAG, errorMessage, exception)

        val intent = Intent(BroadcastActions.VPN_SERVICE_ERROR).apply {
            putExtra("errorMessage", errorMessage)
        }
        Log.w(LOG_TAG, "Sending broadcast on error: $errorMessage")
        sendBroadcast(intent)

        stopSelf()
    }

    override fun onDestroy() {
        Log.w(LOG_TAG, "!!!!!!! Got to onDestroy !!!!!!!")
        super.onDestroy()

        disconnectVpn()

        unregisterReceiver(broadcastReceiver)

        val intent = Intent(BroadcastActions.VPN_SERVICE_STOPPED)
        sendBroadcast(intent)
        Log.w(LOG_TAG, "broadcast ${BroadcastActions.VPN_SERVICE_STOPPED}")
    }

    override fun onRevoke() {
        super.onRevoke()
        vpnConnected.set(false)
        stopSelf()
    }
}
