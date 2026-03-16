package io.github.levldev.blog.dao;

import io.github.levldev.blog.model.Comment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcCommentDao implements CommentDao {

    private final JdbcClient jdbcClient;

    public JdbcCommentDao(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Comment create(Comment comment) {
        String sql = "INSERT INTO public.\"comments\" (post_id, \"text\") VALUES (:postId, :text)";

        KeyHolder kh = new GeneratedKeyHolder();

        jdbcClient.sql(sql)
                .param("postId", comment.getPostId())
                .param("text", comment.getText())
                .update(kh);

        var keys = kh.getKeys();
        if (keys == null) throw new IllegalStateException("No generated id returned");

        Long id = ((Number) keys.get("id")).longValue();

        comment.setId(id);
        return comment;
    }

    @Override
    public Optional<Comment> findById(long id) {
        String sql = "SELECT id, post_id, \"text\", created_at FROM public.\"comments\" WHERE id = :id";
        return jdbcClient.sql(sql)
                .param("id", id)
                .query((rs, rowNum) -> {
                    Comment comment = new Comment();
                    comment.setId(rs.getLong("id"));
                    comment.setPostId(rs.getLong("post_id"));
                    comment.setText(rs.getString("text"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) {
                        comment.setCreatedAt(created.toLocalDateTime());
                    }
                    return comment;
                })
                .optional();
    }

    @Override
    public List<Comment> findByPostId(long postId) {
        String sql = "SELECT id, post_id, \"text\", created_at FROM public.\"comments\" WHERE post_id = :postId ORDER BY id ASC";
        return jdbcClient.sql(sql)
                .param("postId", postId)
                .query((rs, rowNum) -> {
                    Comment comment = new Comment();
                    comment.setId(rs.getLong("id"));
                    comment.setPostId(rs.getLong("post_id"));
                    comment.setText(rs.getString("text"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) {
                        comment.setCreatedAt(created.toLocalDateTime());
                    }
                    return comment;
                })
                .list();
    }

    @Override
    public Comment update(Comment comment) {
        String sql = "UPDATE public.\"comments\" SET \"text\" = :text WHERE id = :id";
        jdbcClient.sql(sql)
                .param("text", comment.getText())
                .param("id", comment.getId())
                .update();
        return comment;
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM public.\"comments\" WHERE id = :id";
        jdbcClient.sql(sql).param("id", id).update();
    }

    @Override
    public void deleteByPostId(long postId) {
        String sql = "DELETE FROM public.\"comments\" WHERE post_id = :postId";
        jdbcClient.sql(sql).param("postId", postId).update();
    }

    @Override
    public long countByPostId(long postId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM public.\"comments\" WHERE post_id = :postId")
                .param("postId", postId)
                .query((rs, rowNum) -> rs.getLong(1))
                .optional()
                .orElse(0L);
    }

    @Override
    public Map<Long, Integer> countByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> params = new HashMap<>();
        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < postIds.size(); i++) {
            String key = "id" + i;
            placeholders.add(":" + key);
            params.put(key, postIds.get(i));
        }
        String sql = "SELECT post_id, COUNT(*) AS cnt FROM public.\"comments\" WHERE post_id IN (" +
                String.join(",", placeholders) + ") GROUP BY post_id";
        Map<Long, Integer> result = new HashMap<>();
        jdbcClient.sql(sql)
                .params(params)
                .query(rs -> {
                    long postId = rs.getLong("post_id");
                    int cnt = rs.getInt("cnt");
                    result.put(postId, cnt);
                });
        return result;
    }
}
