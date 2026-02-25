package io.github.levldev.blog.dao;

import io.github.levldev.blog.model.Comment;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcCommentDao implements CommentDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCommentDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Comment create(Comment comment) {
        String sql = "INSERT INTO public.\"comments\" (post_id, \"text\") VALUES (?, ?)";

        KeyHolder kh = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] { "id" });
            ps.setLong(1, comment.getPostId());
            ps.setString(2, comment.getText());
            return ps;
        }, kh);

        Long id = kh.getKeyAs(Long.class);
        if (id == null) throw new IllegalStateException("No generated id returned");

        comment.setId(id);
        return comment;
    }

    @Override
    public Optional<Comment> findById(long id) {
        String sql = "SELECT id, post_id, \"text\", created_at FROM public.\"comments\" WHERE id = ?";
        try {
            Comment c = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Comment comment = new Comment();
                comment.setId(rs.getLong("id"));
                comment.setPostId(rs.getLong("post_id"));
                comment.setText(rs.getString("text"));
                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    comment.setCreatedAt(created.toLocalDateTime());
                }
                return comment;
            }, id);
            return Optional.ofNullable(c);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Comment> findByPostId(long postId) {
        String sql = "SELECT id, post_id, \"text\", created_at FROM public.\"comments\" WHERE post_id = ? ORDER BY id ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Comment comment = new Comment();
            comment.setId(rs.getLong("id"));
            comment.setPostId(rs.getLong("post_id"));
            comment.setText(rs.getString("text"));
            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                comment.setCreatedAt(created.toLocalDateTime());
            }
            return comment;
        }, postId);
    }

    @Override
    public Comment update(Comment comment) {
        String sql = "UPDATE public.\"comments\" SET \"text\" = ? WHERE id = ?";
        jdbcTemplate.update(sql, comment.getText(), comment.getId());
        return comment;
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM public.\"comments\" WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void deleteByPostId(long postId) {
        String sql = "DELETE FROM public.\"comments\" WHERE post_id = ?";
        jdbcTemplate.update(sql, postId);
    }

    @Override
    public long countByPostId(long postId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.\"comments\" WHERE post_id = ?", Long.class, postId);
        return count;
    }

    @Override
    public Map<Long, Integer> countByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(postIds.size(), "?"));
        String sql = "SELECT post_id, COUNT(*) AS cnt FROM public.\"comments\" WHERE post_id IN (" + placeholders + ") GROUP BY post_id";
        Map<Long, Integer> result = new HashMap<>();
        jdbcTemplate.query(sql, postIds.toArray(), (rs) -> {
            long postId = rs.getLong("post_id");
            int cnt = rs.getInt("cnt");
            result.put(postId, cnt);
        });
        return result;
    }
}