package com.example.backend.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import javax.sql.DataSource;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired DataSource ds;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    // ★ SecurityConfig が要求する依存をテスト用にモック
    @MockitoBean com.example.backend.web.log.AccessLogFilter accessLogFilter;
    @MockitoBean com.example.backend.web.log.RequestIdFilter requestIdFilter;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Test
    void posts_createdAt_is_timestamptz_notnull_and_default_now() throws Exception {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("""
                 SELECT udt_name, is_nullable, column_default
                   FROM information_schema.columns
                  WHERE table_schema = current_schema()
                    AND table_name = 'posts'
                    AND column_name = 'created_at'
             """);
             ResultSet rs = ps.executeQuery()) {

            assertTrue(rs.next(), "posts.created_at が見つからない");
            assertEquals("timestamptz", rs.getString("udt_name"));     // 型
            assertEquals("NO", rs.getString("is_nullable"));           // NOT NULL

            var def = rs.getString("column_default").toLowerCase();    // デフォルト
            assertTrue(def.contains("now()") || def.contains("current_timestamp"),
                    "column_default が now() / current_timestamp ではない: " + def);
        }
    }
}
