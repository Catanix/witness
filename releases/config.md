# Model setup (config.md)

Witness ships with **no embedded keys or models** — the pipeline runs through **your own** ASR and
LLM endpoints. Everything is entered in the app: **Settings** (gear in the phone home header).
With an empty config the pipeline reports "API config is empty" and makes no network requests.

## What you need

Two models:

1. **ASR** (speech-to-text) — transcribes audio into text with speaker separation.
2. **LLM** — generates a title, summary, key points and tasks from the transcript.

For each model you need: an **endpoint URL**, a **model name** and an **API key**.

## Settings fields

| Field | What to enter | Example |
|---|---|---|
| **ASR URL** | speech recognition endpoint | `https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation` |
| **ASR model** | ASR model name | `qwen-audio-3.0-asr-flash` / `qwen3-asr-flash` |
| **LLM URL** | OpenAI-compatible API base URL | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| **LLM model** | LLM model name | `qwen-max` / `gpt-4o` |
| **ASR key** | ASR Bearer key | `sk-...` |
| **LLM key** | LLM Bearer key | `sk-...` |
| **Language** | interface and summary language | EN / RU |

If ASR and LLM are from the same provider, you can use the same key in both fields.

## ASR: two supported protocols

The protocol is detected automatically from the **ASR URL**:

- **DashScope** — if the URL contains `multimodal-generation`. The request uses the DashScope
  protocol (`input.messages` with `input_audio`, response `output.text` + segments).
  Example: Qwen audio ASR on DashScope / a compatible MaaS endpoint.
- **OpenAI-compatible** — any other URL. Request `POST {ASR_URL}/chat/completions` with audio as
  `input_audio` (base64). Works with any ASR that accepts OpenAI-compatible audio input.

> Audio is recorded as WAV 16 kHz mono and encoded as a base64 data-URI.

## LLM: OpenAI-compatible `/chat/completions`

The LLM client sends `POST {LLM_URL}/chat/completions` (Bearer key, `temperature 0.3`) and expects
**strict JSON** of the form:

```json
{
  "title": "short title (<=60 chars)",
  "summary": "brief description, 2-4 sentences",
  "points": ["point 1", "point 2", "..."],
  "tasks": [{"text": "task", "speaker": "speaker"}],
  "segments": [{"speaker": "S1", "text": "utterance"}]
}
```

Any model that follows instructions and returns JSON works: Qwen (`qwen-max`, `qwen-plus`, …),
GPT-4o, Claude via an OpenAI-compatible gateway, local models behind an OpenAI-compatible server
(vLLM, llama.cpp server, LM Studio, etc.). The summary/points/tasks language matches the interface
language (EN or RU).

## Minimal working example (Qwen / DashScope)

```
ASR URL   = https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation
ASR model = qwen3-asr-flash
LLM URL   = https://dashscope.aliyuncs.com/compatible-mode/v1
LLM model = qwen-max
ASR key   = sk-<your-dashscope-key>
LLM key   = sk-<your-dashscope-key>
```

Any other provider works the same way — the ASR must accept audio, and the LLM must return JSON
matching the schema above.

## Verify

1. Fill in the settings fields and save.
2. Import a short WAV (import menu) or record a phrase on the phone.
3. The recording status should progress `RECEIVED → TRANSCRIBING → ANALYZING → DONE`, with a title
   and summary appearing. On `FAILED`, check the URL/key/model name (the error text is shown in the card).

## Where the config is stored

Settings live in DataStore `witness_settings` (keys `asr_url`, `asr_model`, `llm_url`, `llm_model`,
`asr_key`, `llm_key`, `lang`). Keys are stored locally on the device and sent nowhere except the
ASR/LLM endpoints you choose.
