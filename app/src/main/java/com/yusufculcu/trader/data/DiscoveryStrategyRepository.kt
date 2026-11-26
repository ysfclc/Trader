package com.yusufculcu.trader.data

import androidx.room.withTransaction
import com.yusufculcu.trader.domain.DiscoveryStrategy
import com.yusufculcu.trader.domain.DiscoveryStrategyRules
import com.yusufculcu.trader.domain.ProofAsset
import kotlinx.coroutines.flow.Flow

/** Validates provenance and persists editable proof assets and discovery intent atomically. */
class DiscoveryStrategyRepository(private val database: TraderDatabase) {
    fun observeProofAssets(): Flow<List<ProofAssetEntity>> = database.proofAssetDao().observeAll()

    fun observeStrategies(): Flow<List<DiscoveryStrategyEntity>> = database.discoveryStrategyDao().observeAll()

    suspend fun getProofAsset(id: String): ProofAsset? =
        database.proofAssetDao().getById(id)?.let { DiscoveryStrategyCodec.decodeAsset(it.payloadJson) }

    suspend fun getStrategy(id: String): DiscoveryStrategy? =
        database.discoveryStrategyDao().getById(id)?.let { DiscoveryStrategyCodec.decodeStrategy(it.payloadJson) }

    suspend fun saveProofAsset(asset: ProofAsset): List<String> = database.withTransaction {
        val errors = DiscoveryStrategyRules.validate(asset).toMutableList()
        val evidenceIds = database.evidenceDao().getAll().map { it.id }.toSet()
        val missingEvidence = asset.claims.flatMap { it.evidenceIds }.filterNot(evidenceIds::contains).distinct()
        if (missingEvidence.isNotEmpty()) errors += "Proof claims reference evidence records that do not exist."
        if (errors.isNotEmpty()) return@withTransaction errors

        val row = ProofAssetEntity(
            id = asset.id,
            schemaVersion = asset.schemaVersion,
            kind = asset.kind.name,
            payloadJson = DiscoveryStrategyCodec.encode(asset),
            createdAt = asset.createdAt,
            updatedAt = asset.updatedAt
        )
        if (database.proofAssetDao().getById(asset.id) == null) database.proofAssetDao().insert(row)
        else database.proofAssetDao().update(row)
        emptyList()
    }

    suspend fun saveStrategy(strategy: DiscoveryStrategy): List<String> = database.withTransaction {
        val assets = database.proofAssetDao().getAll().map { DiscoveryStrategyCodec.decodeAsset(it.payloadJson) }
        val errors = DiscoveryStrategyRules.validate(strategy, assets)
        if (errors.isNotEmpty()) return@withTransaction errors

        val row = DiscoveryStrategyEntity(
            id = strategy.id,
            proofAssetId = strategy.proofAssetId,
            schemaVersion = strategy.schemaVersion,
            payloadJson = DiscoveryStrategyCodec.encode(strategy),
            createdAt = strategy.createdAt,
            updatedAt = strategy.updatedAt
        )
        if (database.discoveryStrategyDao().getById(strategy.id) == null) database.discoveryStrategyDao().insert(row)
        else database.discoveryStrategyDao().update(row)
        emptyList()
    }
}
