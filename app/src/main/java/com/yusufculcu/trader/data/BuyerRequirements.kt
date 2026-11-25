package com.yusufculcu.trader.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.UUID

object BuyerRequirementState {
    const val DRAFT="DRAFT"; const val OPEN="OPEN"; const val PAUSED="PAUSED"; const val FULFILLED="FULFILLED"; const val CLOSED="CLOSED"
    val ALL=setOf(DRAFT,OPEN,PAUSED,FULFILLED,CLOSED)
}
@Entity(tableName="buyer_requirements", foreignKeys=[ForeignKey(entity=OrganizationEntity::class,parentColumns=["id"],childColumns=["organizationId"],onDelete=ForeignKey.CASCADE)], indices=[Index("organizationId"),Index("status"),Index("updatedAt")])
data class BuyerRequirementEntity(@PrimaryKey val id:String,val organizationId:String,val title:String,val description:String?,val status:String,val destinationCountry:String?,val destinationCity:String?,val requiredByText:String?,val currency:String?,val budgetMinorUnits:Long?,val notes:String?,val createdAt:Long,val updatedAt:Long)
@Entity(tableName="buyer_requirement_items", foreignKeys=[ForeignKey(entity=BuyerRequirementEntity::class,parentColumns=["id"],childColumns=["requirementId"],onDelete=ForeignKey.CASCADE)], indices=[Index("requirementId")])
data class BuyerRequirementLineItemEntity(@PrimaryKey val id:String,val requirementId:String,val productName:String,val specification:String?,val quantity:String?,val unit:String?,val targetUnitPriceMinorUnits:Long?,val createdAt:Long)
@Entity(tableName="buyer_requirement_evidence", foreignKeys=[ForeignKey(entity=BuyerRequirementEntity::class,parentColumns=["id"],childColumns=["requirementId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=OrganizationSourceEntity::class,parentColumns=["id"],childColumns=["sourceId"],onDelete=ForeignKey.CASCADE)],indices=[Index("requirementId"),Index("sourceId"),Index(value=["requirementId","sourceId"],unique=true)])
data class BuyerRequirementEvidenceEntity(@PrimaryKey val id:String,val requirementId:String,val sourceId:String,val linkedAt:Long)

@Dao interface BuyerRequirementDao {
 @Query("SELECT * FROM buyer_requirements WHERE organizationId=:organizationId ORDER BY updatedAt DESC") fun observeForOrganization(organizationId:String):Flow<List<BuyerRequirementEntity>>
 @Query("SELECT * FROM buyer_requirements") suspend fun getAll():List<BuyerRequirementEntity>
 @Query("SELECT * FROM buyer_requirements WHERE id=:id LIMIT 1") suspend fun getById(id:String):BuyerRequirementEntity?
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insert(row:BuyerRequirementEntity)
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertAll(rows:List<BuyerRequirementEntity>)
 @Update suspend fun update(row:BuyerRequirementEntity)
 @Query("DELETE FROM buyer_requirements WHERE id=:id") suspend fun deleteById(id:String):Int
 @Query("DELETE FROM buyer_requirements") suspend fun deleteAll()
}
@Dao interface BuyerRequirementLineItemDao {
 @Query("SELECT * FROM buyer_requirement_items WHERE requirementId=:requirementId ORDER BY createdAt,id") fun observeForRequirement(requirementId:String):Flow<List<BuyerRequirementLineItemEntity>>
 @Query("SELECT * FROM buyer_requirement_items") suspend fun getAll():List<BuyerRequirementLineItemEntity>
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insert(row:BuyerRequirementLineItemEntity)
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertAll(rows:List<BuyerRequirementLineItemEntity>)
 @Query("DELETE FROM buyer_requirement_items WHERE id=:id") suspend fun deleteById(id:String):Int
 @Query("DELETE FROM buyer_requirement_items") suspend fun deleteAll()
}
@Dao interface BuyerRequirementEvidenceDao {
 @Query("SELECT * FROM buyer_requirement_evidence WHERE requirementId=:requirementId ORDER BY linkedAt") fun observeForRequirement(requirementId:String):Flow<List<BuyerRequirementEvidenceEntity>>
 @Query("SELECT * FROM buyer_requirement_evidence") suspend fun getAll():List<BuyerRequirementEvidenceEntity>
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insert(row:BuyerRequirementEvidenceEntity)
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertAll(rows:List<BuyerRequirementEvidenceEntity>)
 @Query("DELETE FROM buyer_requirement_evidence WHERE id=:id") suspend fun deleteById(id:String):Int
 @Query("DELETE FROM buyer_requirement_evidence") suspend fun deleteAll()
}
data class BuyerRequirementDraft(val title:String,val description:String="",val status:String=BuyerRequirementState.DRAFT,val destinationCountry:String="",val destinationCity:String="",val requiredByText:String="",val currency:String="",val budgetMinorUnits:Long?=null,val notes:String="")
data class BuyerRequirementLineItemDraft(val productName:String,val specification:String="",val quantity:String="",val unit:String="",val targetUnitPriceMinorUnits:Long?=null)

/** User-authored requirements with precise line-item quantities and source links constrained to the same dossier. */
class BuyerRequirementRepository(private val db:TraderDatabase) {
 fun observeForOrganization(id:String)=db.buyerRequirementDao().observeForOrganization(id)
 fun observeItems(id:String)=db.buyerRequirementLineItemDao().observeForRequirement(id)
 fun observeEvidence(id:String)=db.buyerRequirementEvidenceDao().observeForRequirement(id)
 suspend fun create(orgId:String,d:BuyerRequirementDraft)=db.withTransaction {
  require(db.organizationDao().getById(orgId)!=null){"Company dossier does not exist."}
  val now=System.currentTimeMillis(); val row=build(orgId,d,UUID.randomUUID().toString(),now)
  db.buyerRequirementDao().insert(row); db.organizationDao().touch(orgId,now); row
 }
 suspend fun update(id:String,d:BuyerRequirementDraft)=db.withTransaction {
  val old=db.buyerRequirementDao().getById(id)?:throw IllegalArgumentException("Buyer requirement no longer exists.")
  val row=build(old.organizationId,d,old.id,old.createdAt).copy(updatedAt=System.currentTimeMillis())
  db.buyerRequirementDao().update(row); db.organizationDao().touch(old.organizationId,row.updatedAt); row
 }
 suspend fun addLineItem(requirementId:String,d:BuyerRequirementLineItemDraft)=db.withTransaction {
  val req=db.buyerRequirementDao().getById(requirementId)?:throw IllegalArgumentException("Buyer requirement does not exist.")
  val quantity=optional(d.quantity,40,"Quantity")?.also { raw ->
   val n=runCatching{BigDecimal(raw)}.getOrElse{throw IllegalArgumentException("Quantity must be a positive decimal number.")}
   require(n>BigDecimal.ZERO && n.scale()<=6 && n.precision()<=18){"Quantity must be a positive decimal with at most six decimal places."}
  }
  require(d.targetUnitPriceMinorUnits==null || d.targetUnitPriceMinorUnits>=0){"Target unit price cannot be negative."}
  val now=System.currentTimeMillis()
  val row=BuyerRequirementLineItemEntity(UUID.randomUUID().toString(),requirementId,required(d.productName,200,"Product or service"),optional(d.specification,2000,"Specification"),quantity,optional(d.unit,40,"Unit"),d.targetUnitPriceMinorUnits,now)
  db.buyerRequirementLineItemDao().insert(row); db.organizationDao().touch(req.organizationId,now); row
 }
 suspend fun linkEvidence(requirementId:String,sourceId:String)=db.withTransaction {
  val req=db.buyerRequirementDao().getById(requirementId)?:throw IllegalArgumentException("Buyer requirement does not exist.")
  val source=db.organizationSourceDao().getById(sourceId)?:throw IllegalArgumentException("Save the evidence source to this company dossier first.")
  require(source.organizationId==req.organizationId){"Evidence must belong to the same company dossier."}
  db.buyerRequirementEvidenceDao().getAll().firstOrNull{it.requirementId==requirementId&&it.sourceId==sourceId}?:BuyerRequirementEvidenceEntity(UUID.randomUUID().toString(),requirementId,sourceId,System.currentTimeMillis()).also{db.buyerRequirementEvidenceDao().insert(it)}
 }
 suspend fun deleteRequirement(id:String)=db.buyerRequirementDao().deleteById(id)>0
 suspend fun deleteLineItem(id:String)=db.buyerRequirementLineItemDao().deleteById(id)>0
 suspend fun deleteEvidenceLink(id:String)=db.buyerRequirementEvidenceDao().deleteById(id)>0
 private fun build(org:String,d:BuyerRequirementDraft,id:String,now:Long):BuyerRequirementEntity {
  require(d.status in BuyerRequirementState.ALL){"Unsupported buyer requirement status."}
  val currency=optional(d.currency,3,"Currency")?.uppercase()?.also{require(Regex("^[A-Z]{3}$").matches(it)){"Currency must be a three-letter ISO code."}}
  require(d.budgetMinorUnits==null||d.budgetMinorUnits>=0){"Budget cannot be negative."}
  return BuyerRequirementEntity(id,org,required(d.title,200,"Requirement title"),optional(d.description,4000,"Description"),d.status,optional(d.destinationCountry,120,"Destination country"),optional(d.destinationCity,120,"Destination city"),optional(d.requiredByText,120,"Required-by timing"),currency,d.budgetMinorUnits,optional(d.notes,2000,"Notes"),now,now)
 }
 private fun required(v:String,max:Int,label:String):String=v.trim().also{require(it.isNotBlank()&&it.length<=max){"$label is required and must be at most $max characters."}}
 private fun optional(v:String,max:Int,label:String):String?=v.trim().also{require(it.length<=max){"$label is too long."}}.ifBlank{null}
}

val MIGRATION_9_10=object:Migration(9,10){
 override fun migrate(db:SupportSQLiteDatabase){
  db.execSQL("""CREATE TABLE IF NOT EXISTS buyer_requirements (id TEXT NOT NULL, organizationId TEXT NOT NULL, title TEXT NOT NULL, description TEXT, status TEXT NOT NULL, destinationCountry TEXT, destinationCity TEXT, requiredByText TEXT, currency TEXT, budgetMinorUnits INTEGER, notes TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(organizationId) REFERENCES organizations(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirements_organizationId ON buyer_requirements (organizationId)")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirements_status ON buyer_requirements (status)")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirements_updatedAt ON buyer_requirements (updatedAt)")
  db.execSQL("""CREATE TABLE IF NOT EXISTS buyer_requirement_items (id TEXT NOT NULL, requirementId TEXT NOT NULL, productName TEXT NOT NULL, specification TEXT, quantity TEXT, unit TEXT, targetUnitPriceMinorUnits INTEGER, createdAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(requirementId) REFERENCES buyer_requirements(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirement_items_requirementId ON buyer_requirement_items (requirementId)")
  db.execSQL("""CREATE TABLE IF NOT EXISTS buyer_requirement_evidence (id TEXT NOT NULL, requirementId TEXT NOT NULL, sourceId TEXT NOT NULL, linkedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(requirementId) REFERENCES buyer_requirements(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(sourceId) REFERENCES organization_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirement_evidence_requirementId ON buyer_requirement_evidence (requirementId)")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirement_evidence_sourceId ON buyer_requirement_evidence (sourceId)")
  db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_buyer_requirement_evidence_requirementId_sourceId ON buyer_requirement_evidence (requirementId, sourceId)")
 }
}
