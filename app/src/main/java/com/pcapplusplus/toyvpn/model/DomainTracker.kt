package com.pcapplusplus.toyvpn.model

class DomainTracker(
    private val timeWindowMillis: Int,
    private val timeProvider: TimeProvider = TimeProvider()
) {
    private val domainData = HashMap<String, MutableList<Long>>()

    fun recordDomain(domain: String) {
        val currentTime = timeProvider.currentTimeMillis()

        if (domainData[domain] != null) {
            domainData[domain]?.add(currentTime)
        } else {
            domainData[domain] = mutableListOf(currentTime)
        }
    }

    fun getTopDomains(count: Int): List<DomainData> {
        val currentTime = timeProvider.currentTimeMillis()
        cleanUpOldDomains(currentTime)

        return domainData.entries
            .sortedByDescending { it.value.size }
            .take(count)
            .map { DomainData(domain = it.key, count = it.value.size) }
    }

    fun clear() {
        domainData.clear()
    }

    private fun cleanUpOldDomains(currentTime: Long) {
        for (entry in domainData) {
            entry.value.removeAll { it < currentTime - timeWindowMillis }
        }

        domainData.keys.removeAll { domainData[it]?.isEmpty() == true }
    }
}