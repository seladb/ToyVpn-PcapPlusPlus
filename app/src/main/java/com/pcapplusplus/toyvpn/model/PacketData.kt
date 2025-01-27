package com.pcapplusplus.toyvpn.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PacketData(
    val isIPv4: Boolean = false,
    val isIPv6: Boolean = false,
    val isTCP: Boolean = false,
    val isUDP: Boolean = false,
    val connectionID: Long? = null,
    val isDNS: Boolean = false,
    val dnsQuery: String? = null,
    val isTLS: Boolean = false,
    val tlsServerName: String? = null,
    val tlsVersion: Int? = null,
    val length: Int,
) : Parcelable
