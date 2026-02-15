# OCR Processor Build Notes

## Platform Support

### ARM64 (Apple Silicon) Development
- **PaddlePaddle** does not provide pre-built wheels for ARM64 Linux
- The `pyproject.toml` has been configured to only install PaddlePaddle on x86_64 platforms
- For development on ARM64, you have two options:
  1. Build for native ARM64 (PaddlePaddle will be skipped, OCR features limited)
  2. Use Docker's multi-platform build to target x86_64 (requires working QEMU emulation)

### Production Deployment (x86_64)
- Production deployments on x86_64 (typical Home Assistant setups) will have full PaddlePaddle support
- Build using: `docker compose build ocr-processor` or `docker build -f Dockerfile .`

## Known Issues

### Docker Desktop `/usr/bin/unpigz` Error on ARM64
**Symptoms:**
```
failed to get stream processor for application/vnd.oci.image.layer.v1.tar+gzip:
fork/exec /usr/bin/unpigz: exec format error
```

**Cause:** Docker Desktop's unpigz binary is corrupted or for the wrong architecture

**Fix:**
1. Open Docker Desktop
2. Settings → Troubleshoot
3. Click "Reset to factory defaults" or "Clean / Purge data"
4. Restart Docker Desktop
5. Verify: `docker pull alpine:latest`
6. Rebuild: `docker compose build ocr-processor`

## Dependencies

### Platform-Specific
- `paddlepaddle>=3.0.0,<4.0.0` - Only on x86_64 (conditional in pyproject.toml)
- `paddleocr>=3.0.0,<4.0.0` - All platforms (gracefully handles missing PaddlePaddle)

### Core Dependencies
- Python 3.11+
- FastAPI
- LangChain + LangGraph
- PyMuPDF (text extraction)
- OpenCV (image processing)

## Build Commands

### Development (with hot reload)
```bash
docker compose up ocr-processor
```

### Production
```bash
docker build -f Dockerfile -t ocr-processor:latest .
docker run -p 8082:8082 -e OLLAMA_HOST=host:11434 ocr-processor:latest
```

### Multi-Platform Build (requires buildx)
```bash
docker buildx build --platform linux/amd64,linux/arm64 -t ocr-processor:latest .
```

Note: ARM64 builds will skip PaddlePaddle installation due to platform marker in pyproject.toml.
