import com.pcapplusplus.toyvpn.model.DomainTracker
import com.pcapplusplus.toyvpn.model.TimeProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DomainTrackerTest {
    @Test
    fun `test adds a domain`() {
        val tracker = DomainTracker(1000)
        tracker.recordDomain("example.com")

        val topDomains = tracker.getTopDomains(10)
        assertEquals(1, topDomains.size)
        assertEquals("example.com", topDomains[0].domain)
        assertEquals(1, topDomains[0].count)
    }

    @Test
    fun `test returns correct top domains`() {
        val tracker = DomainTracker(1000)
        tracker.recordDomain("example.com")
        tracker.recordDomain("example.com")
        tracker.recordDomain("test.com")

        val topDomains = tracker.getTopDomains(10)

        assertEquals(2, topDomains.size)
        assertEquals("example.com", topDomains[0].domain)
        assertEquals(2, topDomains[0].count)
        assertEquals("test.com", topDomains[1].domain)
        assertEquals(1, topDomains[1].count)
    }

    @Test
    fun `test return only top domains`() {
        val tracker = DomainTracker(1000)

        for (i in 1..3) {
            tracker.recordDomain("example.com")
        }

        for (i in 1..2) {
            tracker.recordDomain("test.com")
        }

        tracker.recordDomain("x.com")

        val topDomains = tracker.getTopDomains(2)

        assertEquals(2, topDomains.size)
        assertEquals("example.com", topDomains[0].domain)
        assertEquals("test.com", topDomains[1].domain)
    }

    @Test
    fun `test removes old domain timestamps`() {
        val timeProvider = mockk<TimeProvider>()
        every { timeProvider.currentTimeMillis() } returns 0L

        val tracker = DomainTracker(timeWindowMillis = 1000, timeProvider = timeProvider)

        tracker.recordDomain("example.com")

        every { timeProvider.currentTimeMillis() } returns 500L

        tracker.recordDomain("example.com")

        var topDomains = tracker.getTopDomains(10)
        assertEquals(1, topDomains.size)
        assertEquals(2, topDomains[0].count)

        every { timeProvider.currentTimeMillis() } returns 1001L

        topDomains = tracker.getTopDomains(10)
        assertEquals(1, topDomains.size)
        assertEquals(1, topDomains[0].count)
    }

    @Test
    fun `test removes old domains`() {
        val timeProvider = mockk<TimeProvider>()
        every { timeProvider.currentTimeMillis() } returns 0L

        val tracker = DomainTracker(timeWindowMillis = 1000, timeProvider = timeProvider)

        tracker.recordDomain("example.com")

        every { timeProvider.currentTimeMillis() } returns 500L

        tracker.recordDomain("test.com")

        // Now test that old domains are removed after the time window
        var topDomains = tracker.getTopDomains(10)
        assertEquals(2, topDomains.size)

        every { timeProvider.currentTimeMillis() } returns 1001L

        topDomains = tracker.getTopDomains(10)
        assertEquals(1, topDomains.size)
        assertEquals("test.com", topDomains[0].domain)

        every { timeProvider.currentTimeMillis() } returns 1501L

        topDomains = tracker.getTopDomains(10)
        assertEquals(0, topDomains.size)
    }

    @Test
    fun `test clear`() {
        val tracker = DomainTracker(1000)
        tracker.recordDomain("www.example.com")
        tracker.recordDomain("test.com")

        assertEquals(2, tracker.getTopDomains(10).size)

        tracker.clear()
        assertEquals(0, tracker.getTopDomains(10).size)
    }
}
