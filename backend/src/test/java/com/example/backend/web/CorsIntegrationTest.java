package com.example.backend.web;

import com.example.backend.config.SecurityConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CORSの統合に必要なWeb層＋Securityのみ起動（DB/Flyway/JPAなし） */
@ActiveProfiles({"test", "cors-test"})
@WebMvcTest(controllers = { CorsTestSupport.DummyController.class })
@AutoConfigureMockMvc(addFilters = true) // SecurityFilterChainを有効に
@Import({ SecurityConfig.class, CorsTestSupport.TestBeans.class })
class CorsIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    static final String OK_ORIGIN = "http://localhost:3000";
    static final String OK_ORIGIN_2 = "http://127.0.0.1:3000";
    static final String NG_ORIGIN = "http://evil.example.com";

    @Test
    void preflight_allows_ok_origin() throws Exception {
        mvc.perform(options("/posts")
                        .header(HttpHeaders.ORIGIN, OK_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, OK_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Matchers.containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, Matchers.containsStringIgnoringCase("authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    void preflight_blocks_ng_origin() throws Exception {
        mvc.perform(options("/posts")
                        .header(HttpHeaders.ORIGIN, NG_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void preflight_allows_put_and_delete_for_ok_origin() throws Exception {
        mvc.perform(options("/posts")
                        .header(HttpHeaders.ORIGIN, OK_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Matchers.containsString("PUT")));

        mvc.perform(options("/posts")
                        .header(HttpHeaders.ORIGIN, OK_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Matchers.containsString("DELETE")));
    }

    @Test
    void simple_get_health_has_cors_headers_for_ok_origin() throws Exception {
        mvc.perform(get("/health")
                        .header(HttpHeaders.ORIGIN, OK_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, OK_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, Matchers.containsString("X-Request-ID")))
                .andExpect(header().string("X-Request-ID", Matchers.notNullValue()));
    }

    @Test
    void simple_get_health_without_origin_is_200_and_no_cors_headers() throws Exception {
        mvc.perform(get("/health")) // ← Originヘッダを付けない
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
    }

    @Test
    void simple_get_health_with_ng_origin_is_403_and_no_cors_headers() throws Exception {
        mvc.perform(get("/health")
                        .header(HttpHeaders.ORIGIN, NG_ORIGIN)) // 許可外 Origin
                .andExpect(status().isForbidden())
                // 拒否時なので許可系CORSヘッダは付かない
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
                // 実装にもよりますが、デフォルトはこのメッセージ
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid CORS request")));
    }

    @Test
    void protected_post_emits_cors_even_when_unauthorized() throws Exception {
        mvc.perform(post("/posts")
                        .header(HttpHeaders.ORIGIN, OK_ORIGIN_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized()) // 認証は失敗でOK（CORSヘッダの有無が主目的）
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, OK_ORIGIN_2))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, Matchers.containsString("X-Request-ID")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}

/** テスト用サポート（依存Beanのスタブ＋ダミーController） */
@Profile("cors-test")
class CorsTestSupport {

    @TestConfiguration
    @Profile("cors-test")
    static class TestBeans {

        /** RequestIdFilterの簡易版（レスポンスにX-Request-IDを入れるだけ） */
        @Bean com.example.backend.web.log.RequestIdFilter requestIdFilter() {
            return new com.example.backend.web.log.RequestIdFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                        throws java.io.IOException, jakarta.servlet.ServletException {
                    res.setHeader("X-Request-ID", "test-" + System.nanoTime());
                    chain.doFilter(req, res);
                }
            };
        }

        /** AccessLogFilterはNo-opスタブで十分 */
        @Bean com.example.backend.web.log.AccessLogFilter accessLogFilter() {
            return new com.example.backend.web.log.AccessLogFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                        throws java.io.IOException, jakarta.servlet.ServletException {
                    chain.doFilter(req, res);
                }
            };
        }

        /** JSON出力するだけのダミー（実装差異あれば必要に応じて調整） */
        @Bean com.example.backend.web.error.ErrorResponseWriter errorResponseWriter(com.fasterxml.jackson.databind.ObjectMapper om) {
            return new com.example.backend.web.error.ErrorResponseWriter(om);
        }
    }

    /** /health と /posts の最小実装（ビジネスロジック不要） */
    @RestController
    @Profile("cors-test")
    static class DummyController {
        @GetMapping("/health")
        public java.util.Map<String,String> health() {
            return java.util.Map.of("status", "ok");
        }
        @PostMapping("/posts")
        public java.util.Map<String,String> create(@RequestBody java.util.Map<String,String> body) {
            // 認証が無いのでSecurityで401になる。ここは到達しない想定。
            return java.util.Map.of("ok", "true");
        }
    }
}
