package com.example.backend.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;

@RestControllerAdvice(basePackages = "com.example.backend.web")
public class GlobalExceptionHandler {

    // RequestIdFilter の定数を使うと表記ゆれを防げます
    private static final String REQ_ID_HEADER = com.example.backend.web.log.RequestIdFilter.HDR;     // 例: "X-Request-ID"
    private static final String MDC_KEY      = com.example.backend.web.log.RequestIdFilter.MDC_KEY;  // 例: "requestId"

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIAE(
            IllegalArgumentException e,
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        if ("cursor decode error".equalsIgnoreCase(e.getMessage())) {
            String rid = resolveRequestId(req, res);
            ensureResponseHeader(res, rid);

            Map<String, Object> body = Map.of(
                    "status", 400,
                    "error", "bad_cursor",
                    "code", "BAD_CURSOR",
                    "requestId", rid
            );
            return ResponseEntity.badRequest().body(body);
        }
        // その他のIAEは必要に応じて
        String rid = resolveRequestId(req, res);
        ensureResponseHeader(res, rid);
        Map<String, Object> body = Map.of(
                "status", 400,
                "error", "bad_request",
                "code", "BAD_REQUEST",
                "requestId", rid
        );
        return ResponseEntity.badRequest().body(body);
    }

    private static String resolveRequestId(HttpServletRequest req, HttpServletResponse res) {
        // ① MDC → ② レスポンスヘッダ → ③ リクエストヘッダ → ④ 新規発行
        String fromMdc = MDC.get(MDC_KEY);
        if (isNotBlank(fromMdc)) return fromMdc;

        String fromRes = res.getHeader(REQ_ID_HEADER);
        if (isNotBlank(fromRes)) return fromRes;

        String fromReq = req.getHeader(REQ_ID_HEADER);
        if (isNotBlank(fromReq)) return fromReq;

        return UUID.randomUUID().toString();
    }

    private static void ensureResponseHeader(HttpServletResponse res, String rid) {
        if (!isNotBlank(res.getHeader(REQ_ID_HEADER))) {
            res.setHeader(REQ_ID_HEADER, rid);
        }
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    // バリデーションエラー（@Valid で 400）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        String rid = resolveRequestId(req, res);
        ensureResponseHeader(res, rid);

        List<Map<String, Object>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of(
                        "field", fe.getField(),
                        "message", Optional.ofNullable(fe.getDefaultMessage()).orElse("invalid")
                ))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("error", "validation");
        body.put("code", "VALIDATION_ERROR");
        body.put("requestId", rid);
        body.put("errors", errors);

        return ResponseEntity
                .status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    // JSON が壊れている等（400）
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(
            HttpMessageNotReadableException e,
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        String rid = resolveRequestId(req, res);
        ensureResponseHeader(res, rid);

        Map<String, Object> body = Map.of(
                "status", 400,
                "error", "invalid_body",
                "code", "INVALID_BODY",
                "requestId", rid
        );
        return ResponseEntity
                .status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }


}
