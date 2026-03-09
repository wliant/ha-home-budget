package com.homebudget.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homebudget.dto.OcrResponseDTO;
import com.homebudget.model.Category;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class OcrProcessorClient {

    private static final Logger logger = LoggerFactory.getLogger(OcrProcessorClient.class);
    private static final int CONNECTION_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 120_000;

    @Value("${OCR_PROCESSOR_URL:http://ocr-processor:8082}")
    private String ocrProcessorUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OcrProcessorClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public OcrResult processReceipt(byte[] fileBytes, String originalFilename, List<Category> categories) {
        return processReceipt(fileBytes, originalFilename, categories, null);
    }

    public OcrResult processReceipt(byte[] fileBytes, String originalFilename, List<Category> categories, Category selectedCategory) {
        logger.info("Calling OCR processor for file: {}, selectedCategory: {}", originalFilename,
                selectedCategory != null ? selectedCategory.getName() : "none");

        String contentType = guessContentType(originalFilename);
        String categoriesJson = buildCategoriesJson(categories);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(fileBytes, originalFilename, contentType));
        body.add("categories", categoriesJson);

        if (selectedCategory != null) {
            body.add("selected_category", buildSelectedCategoryJson(selectedCategory));
        }

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<OcrResponseDTO> response = restTemplate.postForEntity(
                    ocrProcessorUrl + "/process",
                    request,
                    OcrResponseDTO.class
            );

            if (response.getBody() != null && response.getBody().getExpenses() != null) {
                logger.info("OCR processor returned {} expenses", response.getBody().getExpenses().size());
                return OcrResult.success(response.getBody());
            }
            return OcrResult.nonRetryableError("OCR processor returned empty response");

        } catch (HttpClientErrorException e) {
            // 4xx errors - non-retryable
            logger.warn("OCR processor returned client error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return OcrResult.nonRetryableError(extractErrorMessage(e.getResponseBodyAsString()));
        } catch (HttpServerErrorException e) {
            // 5xx errors - retryable
            logger.warn("OCR processor returned server error: {}", e.getStatusCode());
            return OcrResult.retryableError("OCR processor temporarily unavailable");
        } catch (ResourceAccessException e) {
            // Connection errors - retryable
            logger.warn("Cannot reach OCR processor: {}", e.getMessage());
            return OcrResult.retryableError("OCR processor unreachable");
        }
    }

    private String buildSelectedCategoryJson(Category category) {
        try {
            Map<String, Object> cat = Map.of("id", category.getId(), "name", category.getName());
            return objectMapper.writeValueAsString(cat);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize selected category ID={}: {}", category.getId(), e.getMessage());
            return "{}";
        }
    }

    private String buildCategoriesJson(List<Category> categories) {
        List<Map<String, Object>> catList = categories.stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName()))
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(catList);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize categories list: {}", e.getMessage());
            return "[]";
        }
    }

    private String extractErrorMessage(String responseBody) {
        try {
            Map<?, ?> error = objectMapper.readValue(responseBody, Map.class);
            Object msg = error.get("message");
            return msg != null ? msg.toString() : "OCR processing failed";
        } catch (JsonProcessingException e) {
            return "OCR processing failed";
        }
    }

    private String guessContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".heic")) return "image/heic";
        if (lower.endsWith(".heif")) return "image/heif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    public static class OcrResult {
        private final OcrResponseDTO response;
        private final String errorMessage;
        private final boolean retryable;
        private final boolean success;

        private OcrResult(OcrResponseDTO response, String errorMessage, boolean retryable, boolean success) {
            this.response = response;
            this.errorMessage = errorMessage;
            this.retryable = retryable;
            this.success = success;
        }

        public static OcrResult success(OcrResponseDTO response) {
            return new OcrResult(response, null, false, true);
        }

        public static OcrResult retryableError(String message) {
            return new OcrResult(null, message, true, false);
        }

        public static OcrResult nonRetryableError(String message) {
            return new OcrResult(null, message, false, false);
        }

        public OcrResponseDTO getResponse() { return response; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isRetryable() { return retryable; }
        public boolean isSuccess() { return success; }
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;
        private final String contentType;

        public NamedByteArrayResource(byte[] bytes, String filename, String contentType) {
            super(bytes);
            this.filename = filename;
            this.contentType = contentType;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}
