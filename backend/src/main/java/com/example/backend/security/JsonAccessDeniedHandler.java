package com.example.backend.security;

import com.example.backend.web.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/** 権限不足の403レスポンスをJSON化 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorWriter;
    private final String requestIdHeaderName;

    public JsonAccessDeniedHandler(ErrorResponseWriter errorWriter, String requestIdHeaderName) {
        this.errorWriter = errorWriter;
        this.requestIdHeaderName = requestIdHeaderName;
    }

    @Override
    public void handle(jakarta.servlet.http.HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (response.isCommitted()) return;

        String rid = response.getHeader(requestIdHeaderName);
        if (rid == null || rid.isBlank()) {
            rid = request.getHeader(requestIdHeaderName);
        }

        errorWriter.write(response, HttpServletResponse.SC_FORBIDDEN,
                "forbidden", "FORBIDDEN", rid);
    }
}
