"""LLM callback handlers for logging chat completions."""

from typing import Any, Dict, List, Optional
from uuid import UUID

from langchain_core.callbacks import AsyncCallbackHandler
from langchain_core.messages import BaseMessage
from langchain_core.outputs import LLMResult

from ocr_processor.logging import get_logger

logger = get_logger(__name__)


class ChatCompletionLogger(AsyncCallbackHandler):
    """Callback handler that logs all LLM chat completions with structured logging."""

    def __init__(self):
        super().__init__()
        self._run_metadata: Dict[UUID, Dict[str, Any]] = {}

    async def on_chat_model_start(
        self,
        serialized: Dict[str, Any],
        messages: List[List[BaseMessage]],
        *,
        run_id: UUID,
        parent_run_id: Optional[UUID] = None,
        tags: Optional[List[str]] = None,
        metadata: Optional[Dict[str, Any]] = None,
        **kwargs: Any,
    ) -> Any:
        """Log when chat model starts processing."""
        # Extract model name from serialized data
        model_name = serialized.get("id", ["unknown"])[-1] if isinstance(serialized.get("id"), list) else "unknown"

        # Get the first message content for logging (usually the prompt)
        prompt_preview = ""
        if messages and messages[0]:
            first_message = messages[0][0]
            content = getattr(first_message, "content", "")
            prompt_preview = (content[:200] + "...") if len(content) > 200 else content

        # Store metadata for this run
        self._run_metadata[run_id] = {
            "model": model_name,
            "prompt_length": len(prompt_preview) if prompt_preview else 0,
            "tags": tags or [],
        }

        logger.info(
            "llm_chat_start",
            run_id=str(run_id),
            parent_run_id=str(parent_run_id) if parent_run_id else None,
            model=model_name,
            prompt_preview=prompt_preview,
            tags=tags,
            metadata=metadata,
        )

    async def on_llm_end(
        self,
        response: LLMResult,
        *,
        run_id: UUID,
        parent_run_id: Optional[UUID] = None,
        tags: Optional[List[str]] = None,
        **kwargs: Any,
    ) -> Any:
        """Log when LLM finishes generating."""
        # Extract response data
        generations = response.generations
        llm_output = response.llm_output or {}

        # Get response text
        response_text = ""
        if generations and generations[0]:
            first_gen = generations[0][0]
            response_text = getattr(first_gen, "text", "")

        # Get token usage if available
        token_usage = llm_output.get("token_usage", {})
        prompt_tokens = token_usage.get("prompt_tokens", 0)
        completion_tokens = token_usage.get("completion_tokens", 0)
        total_tokens = token_usage.get("total_tokens", 0)

        # Get model info from stored metadata
        run_meta = self._run_metadata.get(run_id, {})
        model_name = run_meta.get("model", "unknown")

        # Preview of response (first 200 chars)
        response_preview = (response_text[:200] + "...") if len(response_text) > 200 else response_text

        logger.info(
            "llm_chat_completion",
            run_id=str(run_id),
            parent_run_id=str(parent_run_id) if parent_run_id else None,
            model=model_name,
            response_preview=response_preview,
            response_length=len(response_text),
            prompt_tokens=prompt_tokens,
            completion_tokens=completion_tokens,
            total_tokens=total_tokens,
            tags=tags,
        )

        # Clean up metadata
        self._run_metadata.pop(run_id, None)

    async def on_llm_error(
        self,
        error: BaseException,
        *,
        run_id: UUID,
        parent_run_id: Optional[UUID] = None,
        tags: Optional[List[str]] = None,
        **kwargs: Any,
    ) -> Any:
        """Log when LLM encounters an error."""
        run_meta = self._run_metadata.get(run_id, {})
        model_name = run_meta.get("model", "unknown")

        logger.error(
            "llm_chat_error",
            run_id=str(run_id),
            parent_run_id=str(parent_run_id) if parent_run_id else None,
            model=model_name,
            error_type=type(error).__name__,
            error_message=str(error),
            tags=tags,
        )

        # Clean up metadata
        self._run_metadata.pop(run_id, None)


# Singleton instance for reuse
_chat_completion_logger = None


def get_chat_completion_logger() -> ChatCompletionLogger:
    """Get or create the singleton ChatCompletionLogger instance."""
    global _chat_completion_logger
    if _chat_completion_logger is None:
        _chat_completion_logger = ChatCompletionLogger()
    return _chat_completion_logger
