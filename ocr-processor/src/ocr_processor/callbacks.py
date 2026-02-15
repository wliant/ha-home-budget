"""LLM and node callback handlers for logging."""

from typing import Any, Dict, List, Optional
from uuid import UUID

from langchain_core.callbacks import BaseCallbackHandler
from langchain_core.messages import BaseMessage
from langchain_core.outputs import LLMResult

from ocr_processor.logging import get_logger

logger = get_logger(__name__)


class OcrProcessingLogger(BaseCallbackHandler):
    """Callback handler that logs LLM chat completions and node execution."""

    def __init__(self):
        super().__init__()
        self._run_metadata: Dict[UUID, Dict[str, Any]] = {}

    def on_chain_start(
        self,
        serialized: Dict[str, Any],
        inputs: Dict[str, Any],
        *,
        run_id: UUID,
        parent_run_id: Optional[UUID] = None,
        tags: Optional[List[str]] = None,
        metadata: Optional[Dict[str, Any]] = None,
        **kwargs: Any,
    ) -> Any:
        """Log when a node/chain starts executing."""
        # Get node name from serialized data or tags
        node_name = serialized.get("name", "unknown")
        if tags and any(tag for tag in tags if tag != "graph"):
            node_name = next((tag for tag in tags if tag != "graph"), node_name)

        logger.info(
            "node_start",
            run_id=str(run_id),
            parent_run_id=str(parent_run_id) if parent_run_id else None,
            node=node_name,
            tags=tags,
        )

    def on_chain_end(
        self,
        outputs: Dict[str, Any],
        *,
        run_id: UUID,
        parent_run_id: Optional[UUID] = None,
        tags: Optional[List[str]] = None,
        **kwargs: Any,
    ) -> Any:
        """Log when a node/chain finishes executing."""
        # Get node name from tags
        node_name = "unknown"
        if tags and any(tag for tag in tags if tag != "graph"):
            node_name = next((tag for tag in tags if tag != "graph"), node_name)

        # Log full OCR results for extract node
        log_data = {
            "run_id": str(run_id),
            "parent_run_id": str(parent_run_id) if parent_run_id else None,
            "node": node_name,
            "tags": tags,
        }

        # Log full extracted text for extract node
        if node_name == "extract" and "extracted_text" in outputs:
            log_data["extracted_text"] = outputs["extracted_text"]
            log_data["line_items"] = outputs.get("line_items", [])
            log_data["receipt_date"] = outputs.get("receipt_date")

        # Log classified expenses for classify node
        if node_name == "classify" and "line_items" in outputs:
            log_data["classified_expenses"] = outputs["line_items"]

        logger.info("node_end", **log_data)

    def on_chat_model_start(
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

        # Get the full prompt content
        prompt = ""
        if messages and messages[0]:
            first_message = messages[0][0]
            prompt = getattr(first_message, "content", "")

        # Store metadata for this run
        self._run_metadata[run_id] = {
            "model": model_name,
            "prompt_length": len(prompt),
            "tags": tags or [],
        }

        logger.info(
            "llm_chat_start",
            run_id=str(run_id),
            parent_run_id=str(parent_run_id) if parent_run_id else None,
            model=model_name,
            prompt=prompt,
            prompt_length=len(prompt),
            tags=tags,
        )

    def on_llm_end(
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

        # Get full response text
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

        logger.info(
            "llm_chat_completion",
            run_id=str(run_id),
            parent_run_id=str(parent_run_id) if parent_run_id else None,
            model=model_name,
            response=response_text,
            response_length=len(response_text),
            prompt_tokens=prompt_tokens,
            completion_tokens=completion_tokens,
            total_tokens=total_tokens,
            tags=tags,
        )

        # Clean up metadata
        self._run_metadata.pop(run_id, None)

    def on_llm_error(
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
_ocr_processing_logger = None


def get_ocr_processing_logger() -> OcrProcessingLogger:
    """Get or create the singleton OcrProcessingLogger instance."""
    global _ocr_processing_logger
    if _ocr_processing_logger is None:
        _ocr_processing_logger = OcrProcessingLogger()
    return _ocr_processing_logger
