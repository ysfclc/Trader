# Trader — Data Model

The model distinguishes a buyer's requirement from a supplier's offer. It does not flatten both into a generic lead record.

## Core entities

### ProblemCase

A user-owned problem, question, objective or situation that needs a useful outcome. This is the general entry point and must not be restricted to commercial opportunities.

Store the original user statement and input references (URL, shared text, file, screenshot, voice transcript or manual note); desired outcome and completion criteria; constraints, urgency, risk/stakes and deadline; user-confirmed context, source-derived facts, inferences, assumptions and unknowns as distinct items; linked evidence, related records, candidate paths and selected workflow specialization; status and privacy/retention controls.

A ProblemCase may link to a trade, service, career or future specialized record, but remains valid without any such link. It is not automatically an opportunity or a task list.

### Goal

A measurable or reviewable desired state associated with a ProblemCase. Store outcome statement, acceptance criteria, priority, constraints and target date if any. Success must be user-confirmed, source-confirmed or otherwise verified; generating advice or a plan is not completion.

### Plan and PlanStep

A proposed or selected sequence of work for a ProblemCase or opportunity. Each step records rationale, dependencies, required capability, inputs, expected postcondition, risk/side-effect level, approval requirement, current state and evidence of completion. Plans can be revised; execution history is not silently rewritten.

### Organization
A company, manufacturer, supplier, buyer, importer, distributor, service provider, employer or other institution. Store legal/trading names separately where known, domains, country, registration identifiers when legitimately sourced, sectors, addresses, languages, status and source-linked claims.

One organization can have multiple roles. A company can be both a supplier and a buyer. Role is a relationship, not a separate duplicate company.

### Person
A person connected to an organization, with name, role, publicly available professional contact routes, language, source, contact basis/permission state and do-not-contact status. Do not infer a private address or personal details.

### Product
A product family or specific SKU. Store original names, normalized category, manufacturer, model/SKU, materials/grades, dimensions, application, technical standards, certifications and catalogue/datasheet references. Keep original manufacturer text.

### BuyerRequirement
A need expressed by a buyer or procurement source. Fields may include:
- requested product/category and application;
- technical specification and accepted equivalents;
- material, grade, dimensions and tolerances;
- standards and required certificates;
- quantity, annual volume, batch size and forecast;
- target price/currency/budget only where stated;
- destination, packaging and delivery terms;
- delivery date/urgency;
- required supplier qualification;
- source, validity date, contact route and verification state.

A requirement can contain several line items. Each line item can be matched independently.

### SupplierOffer
A manufacturer's or supplier's stated ability or willingness to supply. Fields may include:
- manufacturer identity and manufacturing/trading role;
- product family, SKU and catalogue;
- specifications, standards and certificates;
- available stock/capacity and its timestamp;
- MOQ, sample conditions and batch flexibility;
- indicative or quoted price, currency and quote validity;
- lead time, export markets and delivery terms;
- payment, warranty, returns and packaging;
- representative/distributor restrictions;
- source and evidence references.

A public website claim is an unverified supplier statement until confirmed directly.

### Source
A registry entry describing a public website, API, directory, EEN profile, catalogue, document, user import or manual source. Includes access policy, adapter version, domain, rate limits, terms, auth needs, source status and last health result.

### Evidence
A source-linked piece of evidence: URL or document ID, captured timestamp, source timestamp if available, raw excerpt or immutable content reference, language, parser version, hash, extracted field, confidence, reviewer decision and verification state.

Evidence must distinguish:
- source statement;
- deterministic normalization;
- AI suggestion;
- human-verified fact;
- expired or contradicted claim.

### Match
A proposed relationship between one buyer requirement (or line item) and one SupplierOffer. It stores:
- matched constraints;
- contradicted constraints;
- unknown constraints;
- evidence supporting each comparison;
- deterministic score components;
- optional AI synonym/translation suggestions;
- version of matching rules;
- reviewer decision and rationale.

Unknown values never count as satisfied constraints. A match cannot be called qualified until all mandatory requirements are verified.

### Quote
A supplier quote or buyer offer, preserving original file/text and terms. Store line items, unit price, quantity, currency, validity, Incoterms, tax treatment, shipping, lead time, payment terms, assumptions and source. A quote is not rewritten by AI.

### Deal
A commercial opportunity connecting buyer, supplier and one or more line-item matches. Tracks introduction authorization, representation/commission terms, quote round, negotiation, order state, delivery checkpoints, win/loss reason and actual revenue/commission when known.

### Communication
An email, message, call note or user handoff. Stores participants, channel, direction, exact content or secure reference, timestamps, delivery/response evidence, approval ID, source and legal basis/consent state. Drafts, approved messages, handed-off content and confirmed sends are distinct states.

### ServiceNeed
A company need backed by public or user-provided evidence. Links the observed problem to one or more service capabilities, the prospect organization/contact, qualification, proposal and outcome.

### JobOpening
A public vacancy or hiring request. Stores role, employer, location, work model, compensation if published, requirements, deadlines, source and last checked. Fit analysis references actual user evidence and explicitly records gaps.

### Task
A human or system task tied to a ProblemCase, Goal, PlanStep, organization, requirement, offer, match, communication, service need, job or deal. Has due date, priority, owner, status and source event.

### Capability

A registered operation Trader can perform through internal logic or an adapter. It declares input/output schema, preconditions, permissions, availability, cost, limits, side-effect classification, approval policy and expected postconditions. A capability declaration is not proof that a provider or device supports it; availability and execution results must be checked.

### Event
Append-only record of material state changes. Includes event ID, timestamp, actor, entity type/ID, action, prior state, new state, evidence IDs and correlation/run ID. Corrections append events rather than erasing history.

## Normalization and identifiers

- Keep original text and language alongside normalized values.
- Normalize product categories using configurable taxonomy/aliases, never only an AI-generated label.
- Store measurements with units and conversion provenance.
- Store currency amounts with ISO currency codes; never compare unlike currencies without a timestamped exchange-rate source.
- Preserve company registration numbers only when obtained from appropriate public/business sources and relevant to the task.
- Deduplicate organizations primarily by verified domain and legal identifier; names alone are insufficient for automatic merging.
- Deduplicate products by manufacturer + SKU/model where possible.
- Every imported row keeps its source-native ID and source ID.

## Matching rules

Mandatory fields are constraints, not merely weights. If a requirement says a certification is mandatory and evidence is absent, the match status is "unknown / needs verification", not pass.

A match explanation must answer:
1. What fits?
2. What does not fit?
3. What cannot yet be checked?
4. Which source evidence supports each conclusion?
5. What question or document would close the gap?

A human may override a suggestion, but the override records actor, time and reason. It does not alter source evidence.

## Data lifecycle

Raw source records are preserved according to source terms and privacy requirements. Personal data is minimized and retained only as necessary for a defined purpose. The system supports user export and a governed deletion process. Append-only audit does not justify indefinite retention of personal data; deletion/anonymization policy must reconcile legal obligations with audit integrity.
