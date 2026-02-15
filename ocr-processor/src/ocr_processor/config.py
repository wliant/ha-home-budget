from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    ollama_host: str = "192.168.1.248:11434"
    text_model: str = "llama3.1:latest"
    max_file_size_mb: int = 10
    log_level: str = "INFO"
    request_timeout: int = 60

    @property
    def ollama_base_url(self) -> str:
        host = self.ollama_host
        if not host.startswith("http"):
            host = f"http://{host}"
        return host

    @property
    def max_file_size_bytes(self) -> int:
        return self.max_file_size_mb * 1024 * 1024

    model_config = {"env_prefix": "", "case_sensitive": False}


settings = Settings()
