package com.example.backend;

import com.example.backend.web.log.AccessLogFilter;
import com.example.backend.web.log.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class BackendApplicationTests {

	@MockitoBean JwtDecoder jwtDecoder;
	@MockitoBean AccessLogFilter accessLogFilter;
	@MockitoBean RequestIdFilter requestIdFilter;

	@Test
	void contextLoads() {}
}
