package com.example.backend.pagination;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CursorCodecTest {

    @Test
    void roundTrip_ok() {
        var at = OffsetDateTime.of(2025, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        long id = Long.MAX_VALUE - 10;

        var token = CursorCodec.encode(new Cursor(at, id));

        var c = CursorCodec.decode(token);
        assertEquals(at, c.at());
        assertEquals(id, c.id());
    }

    @Test
    void invalidToken_throws() {
        assertThrows(IllegalArgumentException.class, () -> CursorCodec.decode("not-base64!"));
    }
}
