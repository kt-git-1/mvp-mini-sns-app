package com.example.backend.web.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
public class ErrorResponseWriter {
    private final ObjectMapper om;

    public ErrorResponseWriter(ObjectMapper om) {
        this.om = om;
    }

    public void write(HttpServletResponse res, int status, String error, String code, String requestId) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        var body = Map.of(
                "error", error,
                "code", code,
                "status", status,
                "requestId", requestId == null ? "" : requestId
        );
        om.writeValue(res.getWriter(), body);
    }
}
