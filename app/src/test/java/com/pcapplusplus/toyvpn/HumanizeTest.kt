package com.pcapplusplus.toyvpn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HumanizeTest {
    @Test
    fun `test numbers less than 10,000`() {
        assertEquals("0", 0.humanize())
        assertEquals("1", 1.humanize())
        assertEquals("42", 42.humanize())
        assertEquals("999", 999.humanize())
        assertEquals("1000", 1000.humanize())
        assertEquals("9999", 9999.humanize())
    }

    @Test
    fun `test numbers in thousands range`() {
        assertEquals("10K", 10000.humanize())
        assertEquals("10.01K", 10010.humanize())
        assertEquals("12.35K", 12345.humanize())
        assertEquals("25.36K", 25359.humanize())
        assertEquals("52K", 51999.humanize())
        assertEquals("40K", 39999.humanize())
        assertEquals("39.1K", 39099.humanize())
        assertEquals("99.95K", 99950.humanize())
        assertEquals("99.99K", 99990.humanize())
        assertEquals("100K", 99997.humanize())
        assertEquals("100K", 100000.humanize())
        assertEquals("150.3K", 150300.humanize())
        assertEquals("150.6K", 150554.humanize())
        assertEquals("999.5K", 999500.humanize())
    }

    @Test
    fun `test boundary cases for K to M transition`() {
        assertEquals("999.9K", 999900.humanize())
        assertEquals("1M", 999950.humanize())
        assertEquals("1M", 999999.humanize())
        assertEquals("1M", 1000000.humanize())
        assertEquals("1M", 1000010.humanize())
    }

    @Test
    fun `test numbers in millions range`() {
        assertEquals("1.001M", 1001000.humanize())
        assertEquals("1.234M", 1234000.humanize())
        assertEquals("1.335M", 1334556.humanize())
        assertEquals("9.999M", 9999000.humanize())
        assertEquals("10.45M", 10446888.humanize())
        assertEquals("50.01M", 50010000.humanize())
        assertEquals("99.99M", 99990000.humanize())
        assertEquals("100M", 100000000.humanize())
        assertEquals("500.5M", 500500000.humanize())
        assertEquals("999.5M", 999500000.humanize())
    }

    @Test
    fun `test boundary cases for M to B transition`() {
        assertEquals("999.9M", 999900000.humanize())
        assertEquals("1B", 999950000.humanize())
        assertEquals("1B", 999999900.humanize())
        assertEquals("1B", 1000000000.humanize())
    }

    @Test
    fun `test numbers in billions range`() {
        assertEquals("1.001B", 1001000000.humanize())
        assertEquals("1.5B", 1500000000.humanize())
        assertEquals("1.672B", 1671900246.humanize())
        assertEquals("2.147B", Int.MAX_VALUE.humanize())
    }
}
