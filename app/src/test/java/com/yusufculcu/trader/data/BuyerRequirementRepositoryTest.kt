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

 @Test fun whitespaceOnlyOptionalFieldsAreStoredAsNull()=runBlocking {
  val org=orgs.createOrganization(OrganizationDraft("Optional Fields Ltd"))
  val req=repo.create(org.id,BuyerRequirementDraft(
   title="Machined brackets", description="  ", destinationCountry=" ", destinationCity="  ",
   requiredByText=" ", currency=" ", notes="  "
  ))
  assertNull(req.description); assertNull(req.destinationCountry); assertNull(req.destinationCity)
  assertNull(req.requiredByText); assertNull(req.currency); assertNull(req.notes)
  val item=repo.addLineItem(req.id,BuyerRequirementLineItemDraft(
   productName="Bracket", specification="  ", quantity=" ", unit="  "
  ))
  assertNull(item.specification); assertNull(item.quantity); assertNull(item.unit)
 }

 @Test fun requirementAndLineItemCorrectionsPreserveIdentityAndAppendHistory()=runBlocking {
  val org=orgs.createOrganization(OrganizationDraft("Correction Buyer"))
  val req=repo.create(org.id,BuyerRequirementDraft("Original requirement",status=BuyerRequirementState.OPEN))
  val item=repo.addLineItem(req.id,BuyerRequirementLineItemDraft("Original part",quantity="10",unit="pcs"))
  val corrected=repo.update(req.id,BuyerRequirementDraft("Corrected requirement",description="Updated drawing revision",status=BuyerRequirementState.PAUSED,currency="EUR",budgetMinorUnits=12500,notes="Confirm revision B"))
  assertEquals(req.id,corrected.id); assertEquals(req.createdAt,corrected.createdAt)
  assertEquals("Corrected requirement",corrected.title); assertEquals(BuyerRequirementState.PAUSED,corrected.status)
  val correctedItem=repo.updateLineItem(item.id,BuyerRequirementLineItemDraft("Revised part","Revision B","12.5","pcs",900))
  assertEquals(item.id,correctedItem.id); assertEquals(item.createdAt,correctedItem.createdAt)
  assertEquals("Revised part",correctedItem.productName); assertEquals("12.5",correctedItem.quantity)
  val events=db.businessActivityDao().getAll()
  val headerEvent = events.single { it.entityId == req.id && it.entityLabel == "Corrected requirement" }
    assertEquals(BusinessActivityAction.STATUS_CHANGED, headerEvent.eventType)
    assertEquals(BuyerRequirementState.OPEN, headerEvent.fromStatus)
    assertEquals(BuyerRequirementState.PAUSED, headerEvent.toStatus)
  assertTrue(events.any{it.entityId==req.id&&it.eventType==BusinessActivityAction.LINE_UPDATED&&it.details=="Revised part"})
  var rejected=false
  try{repo.updateLineItem(item.id,BuyerRequirementLineItemDraft("Bad part",quantity="-1"))}catch(_:IllegalArgumentException){rejected=true}
  assertTrue(rejected)
 }
}
