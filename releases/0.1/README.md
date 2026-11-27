# Witness 0.1

First public build. **No embedded keys or models** — ASR/LLM endpoints, models and keys are entered
in the app settings ([config.md](../config.md)).

## Files

| File | Device | Size |
|---|---|---|
| `witness-mobile-0.1.0.apk` | Phone (Android 8.0+) | ~15 MB |
| `witness-wear-0.1.0.apk` | Watch (Wear OS 3.0+) | ~25 MB |
| `SHA256SUMS.txt` | checksums | — |

Builds are signed with a debug key (for sideload installation). Verify integrity:

```bash
sha256sum -c SHA256SUMS.txt
```

## What's inside

- Recording on phone and watch, audio import.
- Auto-delivery watch → phone over the Wear OS Data Layer with an offline queue.
- Transcription with diarization + LLM summary/points/tasks (your models).
- EN/RU, `.md` export.

## Installation

See [install.md](../install.md) (phone + watch, for humans and AI agents).

## License

[PolyForm Noncommercial 1.0.0](../../LICENSE) — non-commercial use.
