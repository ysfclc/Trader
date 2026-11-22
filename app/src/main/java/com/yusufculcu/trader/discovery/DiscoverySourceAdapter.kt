package com.yusufculcu.trader.discovery

/** Common read contract for configured source instances. */
interface DiscoverySourceAdapter {
    val adapterId: String
    suspend fun fetch(source: DiscoverySourceConfig): List<DiscoveryDocument>

    /** Query-aware sources override this; feeds retain their ordinary fetch behavior. */
    suspend fun search(source: DiscoverySourceConfig, query: String): List<DiscoveryDocument> = fetch(source)
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
            DiscoveryAdapterRegistry(listOf(RssAtomFeedAdapter(), DuckDuckGoDiscoveryAdapter()))
    }
}

/** Bridges the one-shot public search adapter into the durable source-run contract. */
class DuckDuckGoDiscoveryAdapter(
    private val engine: DuckDuckGoHtmlSearch = DuckDuckGoHtmlSearch()
) : DiscoverySourceAdapter {
    override val adapterId: String = ADAPTER_ID

    override suspend fun fetch(source: DiscoverySourceConfig): List<DiscoveryDocument> {
        throw IllegalStateException("A search objective is required for public web search.")
    }

    override suspend fun search(source: DiscoverySourceConfig, query: String): List<DiscoveryDocument> {
        val response = engine.search(PublicSearchRequest(query, limit = MAX_RESULTS))
        return response.results.map { result ->
            DiscoveryDocument(
                id = "$ADAPTER_ID:${result.url}",
                sourceId = response.sourceId,
                sourceName = source.name,
                nativeId = result.url,
                title = result.title,
                url = result.url,
                summary = result.snippet,
                publishedAt = null,
                retrievedAt = result.retrievedAt
            )
        }
    }

    companion object {
        const val ADAPTER_ID = DuckDuckGoHtmlSearch.SOURCE_ID
        const val PUBLIC_SEARCH_SOURCE_ID = "duckduckgo-public-search"
        const val MAX_RESULTS = 10

        fun publicSearchSource() = DiscoverySourceConfig(
            id = PUBLIC_SEARCH_SOURCE_ID,
            adapterId = ADAPTER_ID,
            name = "DuckDuckGo public search",
            feedUrl = "https://html.duckduckgo.com/html/"
        )
    }
}
