package com.yusufculcu.trader.data
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class BuyerRequirementRepositoryTest {
 private val context:Context=ApplicationProvider.getApplicationContext()
 private val db=Room.inMemoryDatabaseBuilder(context,TraderDatabase::class.java).allowMainThreadQueries().build()
 private val orgs=OrganizationRepository(db); private val repo=BuyerRequirementRepository(db)
 @After fun close(){db.close()}
 @Test fun storesRequirementItemsAndSameDossierEvidence()=runBlocking {
  val org=orgs.createOrganization(OrganizationDraft("Acme"))
  val source=orgs.addSource(org.id,OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE,"RFQ","https://acme.example/rfq"))
  val req=repo.create(org.id,BuyerRequirementDraft("CNC housings","Batch order",BuyerRequirementState.OPEN,"Poland","Kraków","Q1 2027","eur",125000))
  assertEquals("EUR",req.currency)
  val item=repo.addLineItem(req.id,BuyerRequirementLineItemDraft("Housing","Aluminium 6082, ±0.05 mm","12.5","pcs",10000))
  assertEquals("12.5",item.quantity)
  val link=repo.linkEvidence(req.id,source.id)
  assertEquals(link.id,repo.linkEvidence(req.id,source.id).id)
  assertEquals(1,repo.observeItems(req.id).first().size); assertEquals(1,repo.observeEvidence(req.id).first().size)
  repo.deleteRequirement(req.id)
  assertTrue(repo.observeItems(req.id).first().isEmpty()); assertTrue(repo.observeEvidence(req.id).first().isEmpty())
 }
 @Test fun rejectsInvalidQuantityCurrencyAndCrossDossierEvidence()=runBlocking {
  val a=orgs.createOrganization(OrganizationDraft("A")); val b=orgs.createOrganization(OrganizationDraft("B"))
  val req=repo.create(a.id,BuyerRequirementDraft("Need parts"))
  val source=orgs.addSource(b.id,OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE,"Source","https://b.example/rfq"))
  var rejected=false; try{repo.linkEvidence(req.id,source.id)}catch(_:IllegalArgumentException){rejected=true}; assertTrue(rejected)
  rejected=false; try{repo.addLineItem(req.id,BuyerRequirementLineItemDraft("Part",quantity="-2"))}catch(_:IllegalArgumentException){rejected=true}; assertTrue(rejected)
  rejected=false; try{repo.create(a.id,BuyerRequirementDraft("Bad",currency="EURO"))}catch(_:IllegalArgumentException){rejected=true}; assertTrue(rejected)
 }
}
