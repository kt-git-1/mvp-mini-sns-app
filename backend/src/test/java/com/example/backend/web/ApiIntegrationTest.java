package com.example.backend.web;

import com.example.backend.TestcontainersConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({
        TestcontainersConfig.class,
        com.example.backend.testsupport.WebTestBeans.class // ← 追加
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void signup_login_post_timeline_ok() throws Exception {
        // signup
        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username":"kaito3",
                                    "password":"password12345"
                                }
                        """))
                        .andExpect(status().isOk());

        // login -> JWT
        var login = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username":"kaito3",
                                    "password":"password12345"
                                }
                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
        String token = om.readTree(login).get("token").asText();

        // post
        mvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "content":"hello world"
                                }
                        """))
                        .andExpect(status().isCreated());

        // timeline
        var tl = mvc.perform(get("/timeline").param("size","20")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.items[0].content").value("hello world"))
                        .andExpect(jsonPath("$.nextCursor").exists())
                        .andReturn().getResponse().getContentAsString();

        JsonNode root = om.readTree(tl);
        String next = root.get("nextCursor").asText();

        // next page（空でもOK・エラーにしない）
        mvc.perform(get("/timeline").param("cursor", next).param("size","20")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk());
    }
}
