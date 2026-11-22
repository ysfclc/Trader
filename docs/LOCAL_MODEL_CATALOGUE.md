# Local Model Catalogue & Management

Trader's Model Library is a curated catalogue of on-device model artifacts, not an unrestricted model downloader. Only releases with a reviewed source, immutable revision, exact artifact URL, expected byte size and SHA-256 digest can become runnable.

Each catalogue entry records a stable Trader ID, intended role, publisher, upstream model page, pinned source revision, artifact filename/format, quantization label, size, digest, RAM preflight threshold, output budget, plain-language description, strengths, limitations, device guidance and license-review status.

## Current curated releases

| Model | Role | Artifact size | Source revision |
|---|---|---:|---|
| Qwen3 0.6B | Fast | 344.7 MB | `litert-community/Qwen3-0.6B` @ `e668717` |
| Qwen3 1.7B | Analyst | 977.2 MB | `litert-community/Qwen3-1.7B` @ `87603f0` |
| Qwen3 4B Thinking | Deep | 2,477.2 MB | `litert-community/Qwen3-4B-Thinking-2507` @ `f8ce479` |

Artifact size is not RAM requirement or performance. The license status for each current entry deliberately requires upstream model-card review; Trader has not completed a license audit. Do not infer context length, language coverage, benchmarks or commercial permissions without source-backed verification.

## Settings and safety

- Active model selection is persisted locally.
- Per-model maximum output-token budget is persisted and clamped between 128 and the catalogue's reviewed maximum.
- CPU is the only inference backend currently wired.
- Cloud fallback is disabled.
- Models live in Trader's private app storage.
- Downloads are resumable where supported and must pass exact-size and SHA-256 checks before becoming runnable.
- RAM and storage checks are preflight guidance, not runtime guarantees.

## Source updates

Current update channel: `reviewed-pinned`. Trader records upstream repository and pinned revision for every artifact. It does not follow moving branch URLs, overwrite installed models or silently install upstream changes.

A catalogue update requires upstream model-card/license review, immutable revision pinning, independent artifact size/hash verification, LiteRT-LM compatibility checks, quality and structured-output tests, device memory/latency/thermal testing, and an explicit user install/update action. Keep a previously verified release available until the candidate passes acceptance.

Automatic upstream polling, update-available notifications, rollback slots, model import/scan, accelerator selection and benchmark history remain future work. The UI must not imply these capabilities already exist.

## Device acceptance

Record device/chipset/RAM, Android version, runtime version, model revision/hash, load time, peak memory where measurable, first-token latency, tokens/second, thermal state, battery impact and failures. A single-device result must not be generalized to all Android devices.


## Upstream metadata checks

The Model Hub can perform a user-triggered metadata check against the Hugging Face model revision API for each curated repository. The check compares the default-branch commit with the pinned source revision and confirms whether the catalogued artifact filename still appears in the repository file list. Results and timestamps are cached in app-private preferences.

The checker first resolves the repository's current default-branch commit, then requests the recursive file tree at that immutable 40-character revision with expanded metadata. It locates the exact catalogued filename and reads its byte size and Git LFS SHA-256 OID.

Statuses: `CURRENT` means the source revision and artifact metadata match the pinned catalogue, or a newer source revision still points to exactly the same bytes; `CANDIDATE_REVIEW_REQUIRED` means the artifact's source-declared size or digest differs and requires human review; `ARTIFACT_MISSING` means the immutable tree does not contain the expected file; `ARTIFACT_UNVERIFIED` means the expected path exists but is not a regular file or its required metadata is missing or malformed; `CHECK_FAILED` means the request or response validation failed.

The SHA-256 shown for a candidate is initially the digest declared by the upstream Git LFS metadata. The Model Hub now offers a separate, user-started **Verify candidate bytes** action. After an explicit confirmation showing the full candidate size, Trader downloads the artifact from its exact 40-character immutable revision into app-private temporary storage, enforces the expected byte count while streaming, hashes the actual bytes with SHA-256, and compares the result with the source-declared digest. The temporary file is deleted after success, failure or cancellation. The operation requires enough free storage for the complete candidate plus a 150 MB reserve and can be cancelled; cancellation disconnects the active request. Redirects must remain on HTTPS, and the artifact request disables content encoding so byte counts apply to the actual model bytes.

A successful result means only that the downloaded bytes match the immutable source metadata. It does not approve the model for use, prove its license, runtime compatibility, quality or device performance, and does not install, activate or repin it. The currently installed model remains untouched. A candidate must still pass model-card/license review, LiteRT-LM compatibility checks, structured-output/quality tests and device memory/latency/thermal testing before a reviewed catalogue change can make it installable.


## Metadata response bounds

The source checker rejects an advertised response larger than 1 MB and also enforces the same limit while streaming bytes. The streaming limit applies even if the server omits or misreports `Content-Length`, preventing an unbounded metadata-body allocation before JSON parsing.


The source metadata request uses a bounded 1 MB streaming reader for both revision and expanded-tree responses. The reader enforces the limit even when the server omits or misreports `Content-Length`.
