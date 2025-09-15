package com.example.backend.security;

import com.example.backend.web.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/** JWT不正・未認証時の401レスポンスをJSON化 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorWriter;
    private final String requestIdHeaderName;
    private final BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

    public JsonAuthenticationEntryPoint(ErrorResponseWriter errorWriter, String requestIdHeaderName) {
        this.errorWriter = errorWriter;
        this.requestIdHeaderName = requestIdHeaderName;
    }

    @Override
    public void commence(jakarta.servlet.http.HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // まずWWW-Authenticateヘッダをデフォルト実装で付与
        delegate.commence(request, response, authException);

        if (response.isCommitted()) return;

        String rid = response.getHeader(requestIdHeaderName);
        if (rid == null || rid.isBlank()) {
            rid = request.getHeader(requestIdHeaderName);
        }

        errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                "unauthorized", "UNAUTHORIZED", rid);
    }
}
