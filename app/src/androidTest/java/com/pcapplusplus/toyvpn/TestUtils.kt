package com.pcapplusplus.toyvpn

import kotlinx.coroutines.delay

suspend fun waitFor(condition: () -> Boolean, timeoutMillis: Long = 5000L) {
    var elapsedTime = 0L
    while (!condition() && elapsedTime < timeoutMillis) {
        delay(10)
        elapsedTime += 10
    }
    if (!condition()) {
        throw AssertionError("Condition not met within timeout")
    }
    return
}

fun String.hexToByteArray(): ByteArray {
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
