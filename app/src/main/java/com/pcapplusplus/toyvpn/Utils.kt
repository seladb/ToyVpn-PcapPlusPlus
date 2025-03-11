package com.pcapplusplus.toyvpn

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import kotlin.collections.ArrayList

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(key: String): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(key, T::class.java)
    } else {
        getParcelableArrayListExtra(key)
    }
}

fun Int.humanize(): String {
    if (this < 10000) {
        return this.toString()
    }

    var (divisor, suffix) =
        when {
            this < 1000000 -> 1000.0 to "K"
            this < 1000000000 -> 1000000.0 to "M"
            else -> 1000000000.0 to "B"
        }

    var value = this / divisor

    if (value >= 999.95) {
        when (suffix) {
            "K" -> {
                value /= 1000
                suffix = "M"
            }
            "M" -> {
                value /= 1000
                suffix = "B"
            }
        }
    }

    val format =
        when {
            value < 10 -> "%.3f"
            value < 100 -> "%.2f"
            else -> "%.1f"
        }

    val formatted = String.format(format, value).toDouble().toString().removeSuffix(".0")
    return "$formatted$suffix"
}
