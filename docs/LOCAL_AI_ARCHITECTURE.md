# Trader Local AI Architecture

Status: accepted direction; first runtime foundation is in progress.
Reference: Memo-2's model lifecycle, context packets, structured planning, routing, local-first operation and bounded execution. Trader remains independently implemented and owns its runtime, models, memory, workflow and browser.

## Product law
Local inference is Trader's default intelligence layer, not an optional chat screen. The local AI supports discovery planning, query generation, evidence extraction, qualification, company research and outreach preparation. External AI is optional escalation only.

## Components
- **Model catalog/hub:** supported formats and runtimes, model metadata, device compatibility, storage/RAM preflight, verified download/import, active-role assignments, load/unload and inference diagnostics.
- **Local inference adapter:** runtime-independent interface with a LiteRT-LM adapter for supported .litertlm models. Runs off the UI thread; explicit model lifecycle; bounded input/output; errors surfaced; no silent cloud fallback.
- **Context Engine:** task-scoped, bounded ContextPacket assembled from proof assets, strategy, source evidence, organization/person, prior decisions and unknowns. Every fact preserves provenance and verification state. Context redaction before any external AI.
- **Orchestrator:** converts intent to validated structured plan, references only registered capabilities, persists plan/run state, enforces budgets/cancellation/recovery and returns structured results.
- **Memory:** local persistent factual memory, discovery strategy history, source quality and outcomes. Separate source facts, user-confirmed facts, model hypotheses and decisions.
- **Task routing:** deterministic rules first; local fast model for extraction/classification/short planning; deeper local model only for complexity; optional frontier consultation only after explicit routing/data boundary. The model cannot grant itself capabilities.
- **Discovery agent:** uses permitted source adapters and Trader-owned browser; extracts evidence with URLs and timestamps, deduplicates, assesses fit, labels unknowns/contradictions, and prepares user-reviewed action.
- **Safety:** AI output is untrusted. Strict schema validation, capability contracts, provenance, bounded loops and time budgets, cancellation, safe recovery. Sending/contacting, account actions, purchases or commitments require separate exact approval. No blind retries for ambiguous side effects.

## Initial model profile
Use the Memo-2 audited LiteRT-LM model catalog as a reference for pinned .litertlm models, but keep a Trader-owned catalog and provenance record:
- Small orchestrator model (~0.6B) for routine extraction and planning.
- Mid-size model (~1.7B) for richer analysis when device memory permits.
- Deep model (~4B) optional, loaded only after RAM preflight; never assumed runnable on every device.
Model sizes and RAM requirements are preflight hints, not guarantees. Start CPU inference to establish correctness, then investigate supported accelerators and fallback separately. Do not force model downloads on first launch.

## Initial milestone sequence
1. **Runtime foundation:** catalog contracts, inference interface, real LiteRT-LM adapter, structured-output validation, context packet contracts and tests.
2. **Model hub:** import/scan/download/resume/hash verification, storage and RAM preflight, model selection/role assignment, diagnostics.
3. **Context engine and memory:** bounded task packets, proof-asset and evidence context, provenance-aware local memory, redaction.
4. **Orchestrator:** validated JSON plans, capability routing, persistence, cancellation and recovery.
5. **Discovery integration:** real public source adapters, query planning, evidence extraction and qualification powered by local inference.
6. **Research/browser integration:** visible research sessions, capture evidence into dossiers, user takeover and verification.
7. **Optional frontier routing:** explicit consent, packet preview, redaction and no silent fallback.

## Acceptance criteria
- Inference uses a verified local model path and runs off main thread.
- Model/runtime errors and missing files are surfaced; no simulated response.
- Model output is validated before it can become a plan or persisted fact.
- Facts retain source and verification metadata; hypotheses remain hypotheses.
- Context is bounded and relevant; sensitive fields are redacted before external routing.
- The app can operate with no model installed, and clearly explains which AI functions are unavailable.
- Device tests report model, runtime, inference duration, memory/compatibility outcome; physical Redmi acceptance remains separate.
- AI never bypasses registered capability limits or human approval requirements.

## Non-goals
- Making Trader depend on Memo, Memo runtime, Memo database, Memo APK, credentials or services.
- Chatbot-first UX.
- Fake AI output, fabricated company results, generic prospect lists or unsupported fit scores.
- Automatic outbound messages or unbounded background agent loops.