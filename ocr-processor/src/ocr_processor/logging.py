import logging
import uuid

import structlog

from ocr_processor.config import settings


def setup_logging() -> None:
    structlog.configure(
        processors=[
            structlog.contextvars.merge_contextvars,
            structlog.stdlib.filter_by_level,
            structlog.stdlib.add_logger_name,
            structlog.stdlib.add_log_level,
            structlog.stdlib.PositionalArgumentsFormatter(),
            structlog.processors.TimeStamper(fmt="iso"),
            structlog.processors.StackInfoRenderer(),
            structlog.processors.format_exc_info,
            structlog.processors.UnicodeDecoder(),
            structlog.processors.JSONRenderer(),
        ],
        wrapper_class=structlog.stdlib.BoundLogger,
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        cache_logger_on_first_use=True,
    )

    log_level = getattr(logging, settings.log_level.upper(), logging.INFO)
    logging.basicConfig(format="%(message)s", level=log_level)


def get_logger(name: str) -> structlog.stdlib.BoundLogger:
    return structlog.get_logger(name)


def generate_correlation_id() -> str:
    return str(uuid.uuid4())


def log_agent_step(
    logger: structlog.stdlib.BoundLogger,
    step_name: str,
    *,
    input_summary: str | None = None,
    output_summary: str | None = None,
    error: str | None = None,
) -> None:
    if error:
        logger.error(
            "agent_step_failed",
            step=step_name,
            input_summary=input_summary,
            error=error,
        )
    else:
        logger.info(
            "agent_step_completed",
            step=step_name,
            input_summary=input_summary,
            output_summary=output_summary,
        )
