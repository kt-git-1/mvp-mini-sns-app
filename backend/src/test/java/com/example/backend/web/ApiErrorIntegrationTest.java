package com.example.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({ com.example.backend.TestcontainersConfig.class,
        com.example.backend.testsupport.WebTestBeans.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApiErrorIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    String username;
    String password = "password12345";
    String token; // 正常なJWT

    // Timelineのsize上限（実装に合わせてサービス層のMAX_SIZEに揃える）
    static final int EXPECTED_MAX_SIZE = 100;

    @BeforeEach
    void setUp() throws Exception {
        username = "u_" + UUID.randomUUID().toString().substring(0, 8);

        // signup
        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());

        // login -> JWT
        var login = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        token = om.readTree(login).get("token").asText();

        // タイムラインに最低1件は載るように1投稿作成（201 or 200 は実装に合わせる）
        mvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"seed post\"}"))
                .andExpect(status().is(Matchers.anyOf(Matchers.is(200), Matchers.is(201))));
    }

    // 1) 未ログインで /posts → 401（拡張JSONとヘッダ検証）
    @Test
    void create_withoutAuth_returns401_withExtendedJson() throws Exception {
        MvcResult res = mvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                .andExpect(header().string("X-Request-Id", Matchers.not(Matchers.blankOrNullString())))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andReturn();

        // ヘッダのX-Request-Id と ボディのrequestId が一致することを検証
        String ridHeader = res.getResponse().getHeader("X-Request-Id");
        JsonNode body = om.readTree(res.getResponse().getContentAsString());
        assertThat(body.get("requestId").asText()).isEqualTo(ridHeader);
    }

    // 2) 281文字投稿 → 400
    @Test
    void create_overMaxLength_returns400() throws Exception {
        String over = "x".repeat(281);

        mvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + over + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("validation"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.errors[0].field").value("content"));
        // メッセージ文言は環境で揺れやすいので固定しない方が無難
    }

    // 3) size が 0, 負数, 大き過ぎ（>MAX） → サーバ側で clamp される
    @Test
    void timeline_size_clamped() throws Exception {
        // size=0
        var r0 = mvc.perform(get("/timeline")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode j0 = om.readTree(r0);
        assertThat(j0.get("items").isArray()).isTrue();
        assertThat(j0.get("items").size()).isBetween(0, EXPECTED_MAX_SIZE);

        // size=-10（負数）
        var rn = mvc.perform(get("/timeline")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "-10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jn = om.readTree(rn);
        assertThat(jn.get("items").size()).isBetween(0, EXPECTED_MAX_SIZE);

        // size > MAX（過大）
        var rbig = mvc.perform(get("/timeline")
                        .header("Authorization", "Bearer " + token)
                        .param("size", String.valueOf(EXPECTED_MAX_SIZE * 10)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jbig = om.readTree(rbig);
        assertThat(jbig.get("items").size()).isLessThanOrEqualTo(EXPECTED_MAX_SIZE);
    }

    // 4) 改ざんJWT（末尾1文字変更）→ 401（拡張JSONとヘッダ検証）
    @Test
    void create_withTamperedJwt_returns401_withExtendedJson() throws Exception {
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        MvcResult res = mvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + tampered)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"tampered\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                .andExpect(header().string("X-Request-Id", Matchers.not(Matchers.blankOrNullString())))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andReturn();

        String ridHeader = res.getResponse().getHeader("X-Request-Id");
        JsonNode body = om.readTree(res.getResponse().getContentAsString());
        assertThat(body.get("requestId").asText()).isEqualTo(ridHeader);
    }

    // 5) 不正カーソル（ランダム文字列）→ 設計どおり（400）
    @Test
    void timeline_withInvalidCursor_returns400() throws Exception {
        mvc.perform(get("/timeline")
                        .header("Authorization", "Bearer " + token)
                        .param("cursor", "!!!totally-invalid-token!!!")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("bad_cursor"))
                .andExpect(jsonPath("$.code").value("BAD_CURSOR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }
}
