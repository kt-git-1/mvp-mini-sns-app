package com.example.backend.repository;

import com.example.backend.TestcontainersConfig;
import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestcontainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryKeysetTest {

    @Autowired UserRepository users;
    @Autowired PostRepository posts;

    @Test
    @Transactional
    void noDup_noMissing_orderedAcrossPages() {
        var u = users.save(new User("kaito", "$2a$10$N70WCDqImvSa3qi7UGu7Z.Sv2Gj/CGL10h2Jh1bv4sDEP/FuZurIG"));
        var base = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        // 30件投入（createdAtの秒が同じものも含む）
        for (int i = 0; i < 30; i++) {
            var p = new Post(u, "p" + i);
            OffsetDateTime ts = (i % 3 == 0) ? base : base.minusSeconds(i / 3);
            ReflectionTestUtils.setField(p, "createdAt", ts);
            posts.save(p);
        }

        int pageSize = 10;
        var at = OffsetDateTime.now(ZoneOffset.UTC).plusYears(1);
        long id = Long.MAX_VALUE;

        Set<Long> seen = new HashSet<>();
        OffsetDateTime cursorAt = at; long cursorId = id;

        for (int page = 0; page < 3; page++) {
            var pageItems = posts.keyset(cursorAt, cursorId, pageSize);

            // ソート順を検証 (created_at desc, id desc)
            var copy = new ArrayList<>(pageItems);
            var sorted = new ArrayList<>(pageItems);
            sorted.sort(Comparator.comparing(Post::getCreatedAt).reversed()
                    .thenComparing(Post::getId, Comparator.reverseOrder()));
            assertEquals(sorted, copy);

            // 重複チェック
            pageItems.forEach(p -> assertTrue(seen.add(p.getId())));

            // 次ページカーソルを更新
            if (!pageItems.isEmpty()) {
                var last = pageItems.get(pageItems.size() - 1);
                cursorAt = last.getCreatedAt();
                cursorId = last.getId();
            }
        }
    }
}
