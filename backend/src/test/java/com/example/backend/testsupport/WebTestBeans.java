package com.example.backend.testsupport;

import com.example.backend.web.error.ErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/** 本番Controllerは使うが、フィルタやエラー出力はテスト用に差し替える */
@TestConfiguration
@Profile("test")
public class WebTestBeans {

    /** X-Request-Id を必ずレスポンスに付与（テストアサーションと値を揃える："X-Request-Id"） */
    @Bean
    public com.example.backend.web.log.RequestIdFilter requestIdFilter() {
        return new com.example.backend.web.log.RequestIdFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                // ★ ApiErrorIntegrationTest は "X-Request-Id"（d小文字）を期待
                res.setHeader("X-Request-Id", "test-" + System.nanoTime());
                chain.doFilter(req, res);
            }
        };
    }

    /** アクセスログは no-op スタブ */
    @Bean
    public com.example.backend.web.log.AccessLogFilter accessLogFilter() {
        return new com.example.backend.web.log.AccessLogFilter() {
            @Override
            public void doFilter(
                    jakarta.servlet.ServletRequest request,
                    jakarta.servlet.ServletResponse response,
                    jakarta.servlet.FilterChain chain)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                // テストでは素通し
                chain.doFilter(request, response);
            }
        };
    }

    /** JSONエラー出力（本番実装をそのまま使う想定ならこれでOK） */
    @Bean
    public ErrorResponseWriter errorResponseWriter(ObjectMapper om) {
        return new ErrorResponseWriter(om);
    }
}
