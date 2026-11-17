# Trader Project State

Last updated: 2026-11-17

## Repository
- Repository: `ysfclc/Trader`
- Primary branch: `main`
- Project: Trader
- Status: **Phase 0 — product definition and repository lock**
- Android source code: not yet implemented.
- APK: not yet built.
- Device acceptance: not yet performed.
- Live discovery or communication integrations: not yet implemented.

## Completed in this checkpoint
- [x] Initialized the previously empty repository with a first commit.
- [x] Added README and product-definition documents.
- [x] Added the phased roadmap, implementation task ledger and this state file.
- [x] Established the first pilot market as industrial tooling and machining between Türkiye and Poland/EU, without hard-coding the product category into the architecture.

## Not yet verified
- [ ] Full document consistency and link audit.
- [ ] Android build and signing pipeline.
- [ ] Local database and capture.
- [ ] Browser/app automation.
- [ ] AI provider adapters or on-device inference.
- [ ] Buyer/supplier matching.
- [ ] Communication adapters and outreach.
- [ ] Any real buyer/supplier match, reply, deal or revenue.

## Known constraints
- Memo is not a dependency. Trader must own its runtime, data and recovery behavior.
- EBO paid AI credits are not a core dependency.
- Unipile is not a core dependency; channel alternatives must be evaluated individually.
- Free-first means core capture, storage, qualification, matching and drafting remain usable without paid API credits. It does not mean unlimited free scraping, messaging or third-party infrastructure.
- External contact and commercial commitments require explicit user approval.
- No claims of live customer discovery or outreach until those actions have actually happened and their evidence is recorded.

## Next checkpoint
1. Audit the locked documents for contradictions and missing links.
2. Inspect reusable code from the user's existing GitHub projects before choosing the Android stack.
3. Create the smallest useful Android vertical slice: capture a buyer need or supplier offer, preserve its source/evidence, and retrieve it offline.
4. Build and test it on the user's Android device before expanding discovery automation.

## Change log
### 2026-11-17 — Initial repository lock
Repository initialized and product definition committed. No application implementation claimed.
