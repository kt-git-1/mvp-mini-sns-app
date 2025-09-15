package com.example.backend.web.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    // 他の層（Advice等）からも使えるように public に
    public static final String HDR = "X-Request-ID";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String rid = req.getHeader(HDR);
        if (rid == null || rid.isBlank()) rid = UUID.randomUUID().toString();

        // レスポンスに必ず付与（本文の requestId と一致させやすい）
        res.setHeader(HDR, rid);

        // try-with-resources でリーク防止（スレッドプールでも安全）
        try (var ignored = MDC.putCloseable(MDC_KEY, rid)) {
            chain.doFilter(req, res);
        }
    }
}
