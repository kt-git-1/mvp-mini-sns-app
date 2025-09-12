package com.example.backend.web;

import com.example.backend.security.AuthUser;
import com.example.backend.service.PostService;
import com.example.backend.web.dto.PostDtos;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
public class PostController {

    private final PostService posts;
    public PostController(PostService posts) { this.posts = posts; }

    @PostMapping("/posts")
    public PostDtos.PostResponse create(@AuthenticationPrincipal AuthUser me, @RequestBody @Valid PostDtos.CreatePostRequest req) {
        return posts.create(me.username(), req.content());
    }
}
