# Trader — Operating Rules

These rules apply to product design, implementation, AI prompts, source adapters, browser/app automation, outbound channels, tests and operations.

## 1. Truth and evidence

- A search result is a lead, not a verified business fact.
- A public company claim is attributed to the company; it is not independently certified by Trader.
- Every factual claim shown to the operator must have a source or be explicitly marked as user-provided, inferred or unknown.
- Dates are stored in UTC and displayed in the relevant local time zone.
- A source's published validity date is different from the date Trader last checked it.
- Do not invent emails, phone numbers, contact names, prices, certificates, MOQs, inventory, capacity, lead times or buyer intent.
- AI output is a suggestion until validated against evidence or approved by a human.
- Sample/demo data must be unmistakably labelled and must never mix with live opportunity results.

## 2. Source and browser behavior

- Prefer official APIs, public business registers, public directories, EEN profiles, manufacturer catalogues and user-provided documents.
- Check site terms, access restrictions, rate limits and relevant robots directives before building an adapter.
- Do not bypass authentication, paywalls, CAPTCHAs, anti-bot measures, technical access controls or rate limits.
- Do not scrape private profiles, private inboxes or information the user is not authorized to access.
- Limit request volume and concurrency; use caching and source-specific backoff.
- Preserve the origin URL and capture timestamp.
- If a source blocks access, show blocked; do not switch to stealth or rotate identities to evade it.
- A browser automation task must show the user the current page and allow takeover.

## 3. Personal data and commercial communications

- Collect and store only contact data needed for a clear commercial or career purpose.
- Record where contact data came from, why it is being used, applicable consent/contact basis, the channel and do-not-contact state.
- Public availability of an email or phone number does not by itself mean all forms of marketing are permitted.
- Before sending, assess the applicable jurisdiction, channel and legal basis. Poland/EU GDPR and electronic-marketing rules may apply, as may Turkish and recipient-country requirements.
- Provide a practical suppression/do-not-contact mechanism and honor requests.
- Do not send bulk messages, deceptive personalization or unsolicited automated sequences.
- Never misrepresent the user's identity, company, relationship, authority or representation.
- No buyer or supplier is described as a client, partner, customer or represented party without evidence and permission.

## 4. Human approval

External actions requiring explicit user approval include:
- sending or submitting a message, RFQ, expression of interest or application;
- posting or commenting under the user's identity;
- making a price, delivery, stock, quality or commercial promise;
- accepting terms, placing orders or committing funds;
- sharing personal or confidential information;
- creating an account or connecting an authenticated channel.

Approval is bound to the exact payload, recipient, channel and purpose. Any material edit invalidates that approval.

The UI must show the actual message and recipient. A generic "approve this task" button is insufficient.

## 5. AI use

AI may:
- extract structured fields from messy text, images or documents;
- translate while preserving original wording;
- suggest product taxonomy/synonyms;
- compare non-binding descriptions;
- draft questions, introductions and applications;
- summarize source evidence and identify gaps.

AI may not:
- create evidence or fill missing commercial terms as if known;
- decide that a mandatory technical specification is satisfied without supporting evidence;
- claim certification, supplier authorization, exclusivity or representation;
- send or submit consequential actions without the approval gate;
- receive credentials, cookies, payment data or unrestricted personal context.

Remote AI use must be explicit, provider-configurable, redacted and cost-visible. The core workflow must still work with AI disabled.

## 6. Financial honesty

- Never show a margin as confirmed if price, freight, duty, tax, FX, insurance, Incoterm or commission basis is missing.
- Distinguish supplier price, buyer price, gross spread, referral fee, commission, landed cost and net profit.
- Mark every estimate and show assumptions.
- Do not promise a specific amount of income or deal probability.
- Do not give legal, tax, customs or regulated financial advice as if Trader is a professional adviser.
- The default commercial role is introduction/sourcing only, under an agreed written arrangement. The user must not imply they can act for either side without authority.

## 7. Security and privacy

- Secrets and tokens live in a dedicated encrypted store backed by Android Keystore.
- Do not log secrets, cookies, OTPs, passwords or full private messages unnecessarily.
- Keep source evidence and personal contact data under separate retention and deletion policies.
- Encrypt private backups and verify them before restore.
- Provide clear export, deletion and backup behavior.
- No hidden recording, surveillance or background capture of third-party app content.
- Explain every sensitive Android permission and make it opt-in.

## 8. Reliability and recovery

- Never discard raw input because parsing failed.
- Every job has a durable state, timestamp, progress counters, warnings and cancellation behavior.
- Retry only operations that are safe and idempotent.
- Do not replay an external send when the result is uncertain.
- Revalidate the source and target before consequential actions.
- Process death must not turn an unapproved action into an approved one.
- Distinguish implemented, locally tested, CI verified, device tested and production proven.
- No simulated results or fake progress.

## 9. Product discipline

- One canonical implementation per capability.
- One source of truth per entity.
- Provider-specific behavior belongs behind adapters.
- Preserve the user's product intent; examples are test fixtures, not new product categories.
- Record decisions and rejected alternatives.
- No feature is complete without acceptance evidence.
