package com.example.backend.service;

import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.AuthUser;
import com.example.backend.web.dto.PostDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class PostService {

    private final PostRepository posts;
    private final UserRepository users;

    public PostService(PostRepository posts, UserRepository users) {
        this.posts = posts;
        this.users = users;
    }

    @Transactional
    public PostDtos.PostResponse create(AuthUser me, String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "content is blank");
        }
        User userRef = users.getReferenceById(me.id());
        var post = posts.save(new Post(userRef, content.strip()));
        return toDto(post);
    }

    private PostDtos.PostResponse toDto(Post p) {
        return new PostDtos.PostResponse(
                p.getId(),
                p.getUser().getId(),
                p.getUser().getUsername(),
                p.getContent(),
                p.getCreatedAt()
        );
    }
}
