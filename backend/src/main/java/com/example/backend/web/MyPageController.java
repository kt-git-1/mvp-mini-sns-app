package com.example.backend.web;

import com.example.backend.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class MyPageController {

    @GetMapping("/mypage")
    public Map<String, Object> mypage(@AuthenticationPrincipal AuthUser me) {
        return Map.of("username", me.username());
    }
}
