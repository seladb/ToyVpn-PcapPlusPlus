package com.pcapplusplus.toyvpn

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pcapplusplus.toyvpn.model.DomainData
import com.pcapplusplus.toyvpn.model.VpnConnectionState

data class TrafficStat(
    val label: String,
    val count: Int,
    val total: Int,
)

@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: ToyVpnViewModel,
) {
    val vpnConnectionState by viewModel.vpnConnectionState.observeAsState(VpnConnectionState.CONNECTED)
    val packetCount by viewModel.packetCount.observeAsState(0)
    val ipv4PacketCount by viewModel.ipv4PacketCount.observeAsState(0)
    val ipv6PacketCount by viewModel.ipv6PacketCount.observeAsState(0)
    val tcpPacketCount by viewModel.tcpPacketCount.observeAsState(0)
    val udpPacketCount by viewModel.udpPacketCount.observeAsState(0)
    val dnsPacketCount by viewModel.dnsPacketCount.observeAsState(0)
    val tlsPacketCount by viewModel.tlsPacketCount.observeAsState(0)
    val tcpConnections by viewModel.tcpConnectionCount.observeAsState(0)
    val udpConnections by viewModel.udpConnectionCount.observeAsState(0)
    val topDnsDomains by viewModel.topDnsDomains.observeAsState()
    val topTlsServerNames by viewModel.topTlsServerNames.observeAsState()

    LaunchedEffect(vpnConnectionState) {
        if (vpnConnectionState == VpnConnectionState.DISCONNECTED) {
            navController.navigate("connect_screen")
        }
    }

    val onDisconnectClicked: () -> Unit = {
        viewModel.disconnectVpn()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    top =
                        WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding(),
                    bottom = 40.dp,
                    start = 16.dp,
                    end = 16.dp,
                )
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Network Traffic Dashboard",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
        )

        StatsCard(
            label = "Total Packets",
            value = packetCount.toString(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        TrafficStatsCard(
            title = "Packet Count by Protocol",
            stats =
                listOf(
                    TrafficStat("IPv4", ipv4PacketCount, packetCount),
                    TrafficStat("IPv6", ipv6PacketCount, packetCount),
                    TrafficStat("TCP", tcpPacketCount, packetCount),
                    TrafficStat("UDP", udpPacketCount, packetCount),
                    TrafficStat("DNS", dnsPacketCount, packetCount),
                    TrafficStat("TLS", tlsPacketCount, packetCount),
                ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        TrafficStatsCard(
            title = "Connections",
            stats =
                listOf(
                    TrafficStat("TCP", tcpConnections, tcpConnections + udpConnections),
                    TrafficStat("UDP", udpConnections, tcpConnections + udpConnections),
                ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        DomainsCard(title = "Top DNS Queries (5 mins)", stats = topDnsDomains)

        Spacer(modifier = Modifier.height(16.dp))

        DomainsCard(title = "Top TLS Hosts (5 mins)", stats = topTlsServerNames)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDisconnectClicked,
            enabled = vpnConnectionState != VpnConnectionState.DISCONNECTED,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(60.dp),
        ) {
            if (vpnConnectionState == VpnConnectionState.DISCONNECTED) {
                Text("Disconnecting...")
            } else {
                Text("Disconnect")
            }
        }
    }
}

@Composable
fun StatsCard(
    label: String,
    value: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp))
        }
    }
}

@Composable
fun TrafficStatsCard(
    title: String,
    stats: List<TrafficStat>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                modifier = Modifier.padding(bottom = 16.dp),
            )
            stats.forEach { stat ->
                TrafficStatRow(stat)
            }
        }
    }
}

@Composable
fun TrafficStatRow(stat: TrafficStat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stat.label,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            modifier = Modifier.wrapContentWidth(Alignment.Start),
        )
        Spacer(modifier = Modifier.width(8.dp))
        ProgressBar(modifier = Modifier.weight(1f), count = stat.count, total = stat.total)
        Text(
            text = stat.count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            modifier = Modifier.align(Alignment.CenterVertically),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun ProgressBar(
    modifier: Modifier,
    count: Int,
    total: Int,
) {
    val progress = (count.toFloat() / total).coerceIn(0f, 1f)

    Box(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .height(8.dp)
                    .padding(end = 8.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun DomainsCard(
    title: String,
    stats: List<DomainData>?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                modifier = Modifier.padding(bottom = 16.dp),
            )
            stats?.forEach { stat ->
                DomainRow(stat)
            }
        }
    }
}

@Composable
fun DomainRow(stat: DomainData) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = "DNS query",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stat.domain,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 22.sp,
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline,
                ),
            modifier =
                Modifier
                    .clickable {
                        val url = "https://${stat.domain}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                    .weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stat.count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStatsScreen() {
    StatsScreen(navController = rememberNavController(), viewModel = viewModel())
}
