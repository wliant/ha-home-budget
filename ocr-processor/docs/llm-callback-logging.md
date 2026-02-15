# LLM Callback Logging

## Overview

The OCR processor implements structured logging for all LLM chat completions using a custom LangChain callback handler. This provides visibility into LLM requests, responses, token usage, and errors.

## Implementation

### Callback Handler: `ChatCompletionLogger`

Located in `src/ocr_processor/callbacks.py`, this async callback handler extends `AsyncCallbackHandler` from LangChain.

**Key Features:**
- Logs LLM invocation start, completion, and errors
- Captures prompt previews (first 200 chars)
- Captures response previews (first 200 chars)
- Tracks token usage (prompt, completion, total)
- Records run IDs for tracing
- Uses structured logging (structlog) for JSON output

### Integration Points

The callback is integrated into all `ChatOllama` instances:

1. **Extract Node** (`extract.py`):
   - Function: `_parse_text_with_llm()`
   - Purpose: Parse raw OCR text into structured receipt data
   - Model: Configured via `settings.text_model`

2. **Classify Node** (`classify.py`):
   - Function: `classify_node()`
   - Purpose: Classify expense items into categories
   - Model: Configured via `settings.text_model`

## Log Output

### Chat Start Event

```json
{
  "event": "llm_chat_start",
  "run_id": "550e8400-e29b-41d4-a716-446655440000",
  "parent_run_id": null,
  "model": "llama3.2:3b",
  "prompt_preview": "Analyze the following receipt text and extract...",
  "tags": [],
  "metadata": {},
  "@timestamp": "2026-02-16T01:45:23.123Z"
}
```

### Chat Completion Event

```json
{
  "event": "llm_chat_completion",
  "run_id": "550e8400-e29b-41d4-a716-446655440000",
  "parent_run_id": null,
  "model": "llama3.2:3b",
  "response_preview": "{\"is_receipt\": true, \"date\": \"2026-02-15\", ...",
  "response_length": 452,
  "prompt_tokens": 234,
  "completion_tokens": 89,
  "total_tokens": 323,
  "tags": [],
  "@timestamp": "2026-02-16T01:45:28.456Z"
}
```

### Chat Error Event

```json
{
  "event": "llm_chat_error",
  "run_id": "550e8400-e29b-41d4-a716-446655440000",
  "parent_run_id": null,
  "model": "llama3.2:3b",
  "error_type": "TimeoutError",
  "error_message": "Request timed out after 30 seconds",
  "tags": [],
  "@timestamp": "2026-02-16T01:45:35.789Z"
}
```

## Configuration

### Enable/Disable Logging

The callback is always enabled by default. To disable LLM logging temporarily:

```python
# In extract.py or classify.py
llm = ChatOllama(
    model=settings.text_model,
    base_url=settings.ollama_base_url,
    timeout=settings.request_timeout,
    # callbacks=[get_chat_completion_logger()],  # Comment out to disable
)
```

### Adjust Log Level

Control log verbosity via environment variables:

```bash
# Development (DEBUG level)
LOG_LEVEL=DEBUG

# Production (INFO level, default)
LOG_LEVEL=INFO

# Minimal logging (WARNING level)
LOG_LEVEL=WARNING
```

## Token Usage Tracking

The callback automatically captures token usage from the LLM response when available:

- **prompt_tokens**: Number of tokens in the input prompt
- **completion_tokens**: Number of tokens in the generated response
- **total_tokens**: Sum of prompt and completion tokens

**Note**: Token usage depends on the LLM provider. Ollama may or may not return token counts depending on the model and configuration.

## Use Cases

### Debugging LLM Responses

Check logs to see what prompts were sent and what responses were received:

```bash
# Filter for completion events
docker compose logs ocr-processor | grep "llm_chat_completion"

# Extract response previews
docker compose logs ocr-processor | jq 'select(.event == "llm_chat_completion") | .response_preview'
```

### Performance Monitoring

Track LLM latency by correlating start and completion events:

```bash
# Get all LLM events for a specific run
docker compose logs ocr-processor | jq 'select(.run_id == "550e8400-e29b-41d4-a716-446655440000")'
```

### Cost Analysis

Sum token usage across all requests (if available):

```bash
# Total tokens used
docker compose logs ocr-processor | jq 'select(.event == "llm_chat_completion") | .total_tokens' | awk '{sum+=$1} END {print sum}'
```

### Error Analysis

Identify LLM failures:

```bash
# All LLM errors
docker compose logs ocr-processor | jq 'select(.event == "llm_chat_error")'

# Group by error type
docker compose logs ocr-processor | jq 'select(.event == "llm_chat_error") | .error_type' | sort | uniq -c
```

## Extending the Callback

To add custom logging or metrics, extend the `ChatCompletionLogger` class:

```python
class CustomChatLogger(ChatCompletionLogger):
    async def on_llm_end(self, response, *, run_id, **kwargs):
        # Call parent implementation
        await super().on_llm_end(response, run_id=run_id, **kwargs)

        # Add custom logic
        # e.g., send metrics to monitoring service
        await send_to_metrics_service({
            "tokens": response.llm_output.get("token_usage", {}),
            "run_id": str(run_id)
        })
```

## Testing

To verify the callback is working:

1. **Check logs during receipt processing**:
   ```bash
   docker compose up ocr-processor
   # Upload a receipt via API
   # Check logs for llm_chat_start and llm_chat_completion events
   ```

2. **Verify JSON structure**:
   ```bash
   docker compose logs ocr-processor | jq 'select(.event | startswith("llm_"))'
   ```

3. **Test error handling**:
   - Stop Ollama server
   - Upload receipt
   - Verify `llm_chat_error` event is logged

## Best Practices

1. **Monitor Log Volume**: LLM logging can generate significant logs. Rotate logs regularly.
2. **Redact Sensitive Data**: The callback logs prompt/response previews. Ensure no PII is logged.
3. **Use Run IDs**: Correlate LLM events with agent execution using `run_id`.
4. **Track Token Usage**: Monitor token consumption to optimize costs (if using paid LLM APIs).

## Related Files

- `src/ocr_processor/callbacks.py` - Callback handler implementation
- `src/ocr_processor/nodes/extract.py` - Extract node with LLM integration
- `src/ocr_processor/nodes/classify.py` - Classify node with LLM integration
- `src/ocr_processor/logging.py` - Structured logging configuration
