package com.yusufculcu.trader.discovery

/** Common read contract for configured source instances. */
interface DiscoverySourceAdapter {
    val adapterId: String
    suspend fun fetch(source: DiscoverySourceConfig): List<DiscoveryDocument>
}

/**
 * Explicit built-in adapter registry. It never downloads or executes provider code at runtime.
 * A configured source selects an installed adapter through its stable adapter ID.
 */
class DiscoveryAdapterRegistry(adapters: Collection<DiscoverySourceAdapter>) {
    private val adaptersById = adapters.associateBy { it.adapterId }

    init {
        require(adaptersById.size == adapters.size) { "Discovery adapter IDs must be unique." }
    }

    fun adapterFor(source: DiscoverySourceConfig): DiscoverySourceAdapter =
        adaptersById[source.adapterId]
            ?: throw FeedSourceException("The source adapter '${source.adapterId}' is not installed or enabled.")

    companion object {
        fun builtIns(): DiscoveryAdapterRegistry =
            DiscoveryAdapterRegistry(listOf(RssAtomFeedAdapter()))
    }
}
