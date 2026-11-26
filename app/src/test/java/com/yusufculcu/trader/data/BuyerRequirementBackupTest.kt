package com.yusufculcu.trader.data
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
@RunWith(RobolectricTestRunner::class)
class BuyerRequirementBackupTest {
 private val context:Context=ApplicationProvider.getApplicationContext()
 @Test fun encryptedBackupRoundTripPreservesRequirementsItemsAndEvidence()=runBlocking {
  val source=Room.inMemoryDatabaseBuilder(context,TraderDatabase::class.java).allowMainThreadQueries().build()
  val target=Room.inMemoryDatabaseBuilder(context,TraderDatabase::class.java).allowMainThreadQueries().build()
  try {
   val orgs=OrganizationRepository(source); val org=orgs.createOrganization(OrganizationDraft("Acme"))
   val evidence=orgs.addSource(org.id,OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE,"RFQ","https://acme.example/rfq"))
   val reqs=BuyerRequirementRepository(source); val req=reqs.create(org.id,BuyerRequirementDraft("CNC housings",status=BuyerRequirementState.OPEN,currency="EUR",budgetMinorUnits=12345))
   reqs.addLineItem(req.id,BuyerRequirementLineItemDraft("Housing","Aluminium 6082","12.5","pcs",2500)); reqs.linkEvidence(req.id,evidence.id)
   val out=ByteArrayOutputStream(); TraderBackupManager(source).exportEncrypted(out,"correct horse battery".toCharArray())
   TraderBackupManager(target).restoreEncrypted(ByteArrayInputStream(out.toByteArray()),"correct horse battery".toCharArray())
   val restored=target.buyerRequirementDao().getAll().single()
   assertEquals("CNC housings",restored.title); assertEquals("EUR",restored.currency)
   assertEquals("12.5",target.buyerRequirementLineItemDao().getAll().single().quantity)
   assertEquals(evidence.id,target.buyerRequirementEvidenceDao().getAll().single().sourceId)
  } finally {source.close();target.close()}
 }
}
