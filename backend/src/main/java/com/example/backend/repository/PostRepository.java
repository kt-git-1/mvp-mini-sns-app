package com.example.backend.repository;

import com.example.backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // Keysetページング
    @Query(value = """
    SELECT p.*
      FROM posts p
     WHERE (p.user_id = :me
            OR p.user_id IN (SELECT followed_id FROM follows WHERE follower_id = :me))
       AND (p.created_at, p.id) < (:cursorAt, :cursorId)
     ORDER BY p.created_at DESC, p.id DESC
     LIMIT :size
    """, nativeQuery = true)
    List<Post> compositeTimeline(
            @Param("me") Long me,
            @Param("cursorAt") OffsetDateTime cursorAt,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );

    @Query(value = """
     SELECT p.*
       FROM posts p
      WHERE (p.created_at < :at)
         OR (p.created_at = :at AND p.id < :id)
      ORDER BY p.created_at DESC, p.id DESC
      LIMIT :limit
    """, nativeQuery = true)
    List<Post> keyset(
            @Param("at") OffsetDateTime at,
            @Param("id") Long id,
            @Param("limit") int limit
    );
}
