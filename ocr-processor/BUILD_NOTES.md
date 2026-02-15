# OCR Processor Build Notes

## Platform Support

### ARM64 (Apple Silicon) Development
- **TesseractOCR** is available on all platforms (ARM64, x86_64, macOS, Windows, Linux)
- No platform-specific dependencies or limitations
- Full OCR features available on all architectures

### Production Deployment
- Works on both x86_64 and ARM64 architectures
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

### System Dependencies
- `tesseract-ocr` - TesseractOCR binary (installed via apt-get)
- `tesseract-ocr-eng` - English language data for Tesseract

### Python Dependencies
- Python 3.11+
- FastAPI
- LangChain + LangGraph
- PyMuPDF (text extraction from PDFs)
- pytesseract (Python wrapper for TesseractOCR)
- Pillow (image processing)

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
