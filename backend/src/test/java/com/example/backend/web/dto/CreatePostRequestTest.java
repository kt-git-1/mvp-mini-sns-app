package com.example.backend.web.dto;

import jakarta.validation.Validation;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreatePostRequestTest {

    record CreatePostRequest(
            @NotBlank
            @Size(max = 280)
            String content) {}

    @Test
    void ok_whenWithin280() {
        var v = Validation.buildDefaultValidatorFactory().getValidator();
        var req = new CreatePostRequest("hello");
        assertTrue(v.validate(req).isEmpty());
    }

    @Test
    void violation_whenBlankOrTooLong() {
        var v = Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(v.validate(new CreatePostRequest(" ".repeat(3))).isEmpty());
        assertFalse(v.validate(new CreatePostRequest("x".repeat(281))).isEmpty());
    }
}
