# Industrial Sourcing Desk

**Product status:** first end-to-end product; CI-verified, physical-device acceptance open  
**Build candidate:** Trader 0.9.0-debug / versionCode 9  
**Pilot:** Türkiye ↔ Poland/EU industrial tooling and machining

## Job to be done

Turn a concrete buyer requirement into a defensible shortlist of potential suppliers without losing technical context, source evidence or the reasons behind the decision.

This is an operator's sourcing desk, not a public marketplace, supplier directory, autonomous broker or CRM. It is designed for made-to-spec B2B manufacturing work such as CNC-machined components, tooling, moulds, fixtures and small-batch production.

## The product journey

1. **Record the buyer.** Create or select the buyer's company dossier and capture the actual requirement. Preserve source pages/documents in that dossier.
2. **Structure the request.** Record each requested product/service, technical specification, quantity, unit, target unit price/currency, destination and required-by timing. Missing information remains missing.
3. **Record supplier candidates.** Create supplier company dossiers from research or manual input. Keep company identity and source snapshots distinct from supplier claims.
4. **Capture an offer.** Record the supplier's stated product/service, specification, material, available quantity, unit, MOQ, unit price/currency, lead time and origin. Link saved sources from that supplier's dossier. The offer is a supplier statement, not independent verification.
5. **Compare.** Trader evaluates each buyer line item against each supplier offer line item using deterministic checks. It does not use AI to turn similar wording into technical equivalence.
6. **Decide.** The operator records one of three decisions for an offer against a buyer requirement: shortlisted, needs information, or not suitable. A rationale is retained and the decision can be revised.
7. **Prepare the handoff.** Share a concise shortlist brief through Android Sharesheet. Trader does not send an RFQ, make a commitment, or claim a message was delivered.

## Comparison contract

| Check | Supported only when | Otherwise |
|---|---|---|
| Product name | Exact normalized wording match | Different/missing wording → unknown |
| Specification | Exact normalized wording match | Different/missing wording → unknown; no tolerance equivalence inferred |
| Capacity | Requested and available quantities parse and units match; stated availability covers request | Missing/different units → unknown; insufficient stated quantity → conflict |
| MOQ | Requested and MOQ quantities parse and units match; requested quantity meets MOQ | Missing/different units → unknown; request below MOQ → conflict |
| Price | Buyer target and supplier price exist in the same currency; supplier price is at/below target | Missing/different currency → unknown; above target → negotiation/review, not automatic rejection |

No automatic unit conversion, FX conversion, synonym matching, certification validation, quality approval, export qualification, lead-time feasibility or supplier verification is claimed. A fully comparable result is **reviewable**, never automatically “qualified”.

## Authority and safety boundaries

- Supplier statements retain their source links and are not represented as independently verified facts.
- A source link is evidence provenance, not proof that the source is true or current.
- The operator's shortlist decision is separate from the deterministic comparison.
- No external RFQ, email, WhatsApp, LinkedIn message, call, quote acceptance or order is sent by this product.
- The share action opens the Android Sharesheet with a draft brief; the user chooses whether and where to share.
- Do not infer buyer authority, supplier representation, certification, capacity beyond stated data, price validity or commercial terms.
- No sample/demo suppliers or fake market data are inserted into live records.

## Current implementation and verification

- Native Android Sourcing destination; buyer requests remain managed in company dossiers.
- Supplier offer header, normalized line items, company-scoped evidence links and persistent human review decisions.
- Room v10→v11 stores supplier offers and sourcing decisions; Room v11→v12 adds append-only business activity history.
- Encrypted workspace backup snapshot v9 preserves all commercial records and business history; snapshots v1–v8 remain backward-readable.
- Buyer requirement and supplier offer status controls are editable in the dossier/Sourcing UI; every transition records its previous and new state.
- Activity combines ProblemCase history with company, contact, claim, requirement, offer and sourcing-decision events. Contact email, phone and name are not copied into event details.
- English, Polish and Turkish UI.
- CI verification for build candidate 0.9.0-debug is pending; no new APK is claimed yet.
- Physical-device install-over-existing, migration/restore and real-world sourcing acceptance are still open.

## Product completion gate

Do not start another product until a real device journey proves: create buyer dossier/request; create a supplier dossier/offer with source evidence; compare multiple offers; record and revise decisions; share a shortlist; record and revise requirement/offer statuses and sourcing decisions; inspect the combined activity history; close/reopen the app without losing records; export and restore the encrypted workspace; and install the next signed APK over the existing app without uninstalling or losing data.
