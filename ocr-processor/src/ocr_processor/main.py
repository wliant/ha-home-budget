import json

import httpx
import structlog
from fastapi import FastAPI, File, Form, UploadFile
from fastapi.responses import JSONResponse

from ocr_processor.config import settings
from ocr_processor.errors import OcrError
from ocr_processor.logging import generate_correlation_id, get_logger, setup_logging
from ocr_processor.models import CategoryInput, ErrorResponse, ProcessResponse

setup_logging()

app = FastAPI(title="Receipt OCR Processor", version="1.0.0")


@app.exception_handler(OcrError)
async def ocr_error_handler(request, exc: OcrError):
    status_code = 503 if exc.retryable else 422
    return JSONResponse(
        status_code=status_code,
        content=ErrorResponse(
            error_code=exc.error_code,
            message=exc.message,
            retryable=exc.retryable,
        ).model_dump(),
    )


@app.get("/health")
async def health():
    ollama_reachable = False
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            resp = await client.get(f"{settings.ollama_base_url}/api/tags")
            ollama_reachable = resp.status_code == 200
    except Exception:
        pass
    return {"status": "healthy", "ollama_reachable": ollama_reachable}


@app.post("/process", response_model=ProcessResponse)
async def process_receipt(
    file: UploadFile = File(...),
    categories: str = Form(...),
):
    from ocr_processor.agent import process_receipt as run_agent

    correlation_id = generate_correlation_id()
    structlog.contextvars.clear_contextvars()
    structlog.contextvars.bind_contextvars(correlation_id=correlation_id)

    logger = get_logger("ocr_processor.api")
    logger.info("process_request_received", filename=file.filename, content_type=file.content_type)

    category_list = [CategoryInput(**c) for c in json.loads(categories)]
    file_bytes = await file.read()
    content_type = file.content_type or ""

    result = await run_agent(file_bytes, content_type, category_list)

    logger.info("process_request_completed", expense_count=len(result.expenses))
    return result
