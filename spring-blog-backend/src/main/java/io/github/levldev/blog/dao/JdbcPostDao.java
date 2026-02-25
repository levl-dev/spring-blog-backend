package io.github.levldev.blog.dao;

import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.service.dto.ImageData;
import io.github.levldev.blog.service.dto.SearchCriteria;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcPostDao implements PostDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPostDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Post create(Post post) {
        String sql = "INSERT INTO public.posts (title, \"text\", likes_count) VALUES (?, ?, ?)";

        KeyHolder kh = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] { "id" });
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getText());
            ps.setInt(3, post.getLikesCount());
            return ps;
        }, kh);

        Long id = kh.getKeyAs(Long.class);
        if (id == null) throw new IllegalStateException("No generated id returned");

        post.setId(id);
        replaceTagsForPost(id, post.getTags());
        return post;
    }

    @Override
    public Optional<Post> findById(long id) {
        String sql = "SELECT id, title, \"text\", created_at, likes_count, image, image_content_type " +
                "FROM public.posts WHERE id = ?";
        try {
            Post post = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Post p = new Post();
                p.setId(rs.getLong("id"));
                p.setTitle(rs.getString("title"));
                p.setText(rs.getString("text"));
                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    p.setCreatedAt(created.toLocalDateTime());
                }
                p.setLikesCount(rs.getInt("likes_count"));
                p.setImage(rs.getBytes("image"));
                p.setImageContentType(rs.getString("image_content_type"));
                return p;
            }, id);
            if (post != null) {
                post.setTags(findTagsForPost(post.getId()));
            }
            return Optional.ofNullable(post);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Post> findPage(SearchCriteria criteria, int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;

        StringBuilder from = new StringBuilder(" FROM public.posts p");
        List<Object> params = new ArrayList<>();

        String where = buildWhereClauseBySearch(criteria, params);

        String sql = "SELECT p.id, p.title, p.\"text\", p.created_at, p.likes_count " +
                from + where +
                " ORDER BY p.id DESC " +
                "LIMIT ? OFFSET ?";

        params.add(pageSize);
        params.add(offset);

        List<Post> posts = jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> {
            Post p = new Post();
            p.setId(rs.getLong("id"));
            p.setTitle(rs.getString("title"));
            p.setText(rs.getString("text"));
            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                p.setCreatedAt(created.toLocalDateTime());
            }
            p.setLikesCount(rs.getInt("likes_count"));
            return p;
        });
        loadTagsForPosts(posts);
        return posts;
    }

    @Override
    public long countBySearch(SearchCriteria criteria) {
        String from = " FROM public.posts p";
        List<Object> params = new ArrayList<>();

        String where = buildWhereClauseBySearch(criteria, params);

        String sql = "SELECT COUNT(*)" + from + where;

        Long count = jdbcTemplate.query(sql, params.toArray(), rs -> {
            if (rs.next()) return rs.getLong(1);
            return 0L;
        });
        return count != null ? count : 0L;
    }

    @Override
    public Post update(Post post) {
        String sql = "UPDATE public.posts SET title = ?, \"text\" = ?, likes_count = ? " +
                "WHERE id = ?";
        jdbcTemplate.update(sql,
                post.getTitle(),
                post.getText(),
                post.getLikesCount(),
                post.getId());
        replaceTagsForPost(post.getId(), post.getTags());
        return post;
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM public.posts WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public int incrementLikes(long id) {
        String updateSql = "UPDATE public.posts SET likes_count = likes_count + 1 WHERE id = ?";
        jdbcTemplate.update(updateSql, id);
        String selectSql = "SELECT likes_count FROM public.posts WHERE id = ?";
        Integer likes = jdbcTemplate.queryForObject(selectSql, Integer.class, id);
        return likes;
    }

    @Override
    public void updateImage(long id, byte[] imageBytes, String imageContentType) {
        String sql = "UPDATE public.posts SET image = ?, image_content_type = ? WHERE id = ?";
        jdbcTemplate.update(sql, imageBytes, imageContentType, id);
    }

    @Override
    public ImageData getImage(long id) {
        String sql = "SELECT image, image_content_type FROM public.posts WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                byte[] imageBytes = rs.getBytes("image");
                String contentType = rs.getString("image_content_type");
                return new ImageData(imageBytes, contentType);
            }, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void replaceTagsForPost(long postId, List<String> tags) {
        jdbcTemplate.update("DELETE FROM public.post_tags WHERE post_id = ?", postId);
        if (tags == null || tags.isEmpty()) {
            return;
        }
        List<String> cleaned = tags.stream()
                .map(tag -> tag == null ? null : tag.trim())
                .filter(tag -> tag != null && !tag.isEmpty())
                .toList();
        if (cleaned.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO public.post_tags (post_id, tag) VALUES (?, ?)",
                cleaned,
                cleaned.size(),
                (ps, tag) -> {
                    ps.setLong(1, postId);
                    ps.setString(2, tag);
                }
        );
    }

    private List<String> findTagsForPost(long postId) {
        return jdbcTemplate.query(
                "SELECT tag FROM public.post_tags WHERE post_id = ? ORDER BY tag",
                (rs, rowNum) -> {
                    String tag = rs.getString("tag");
                    return tag != null ? tag.trim() : null;
                },
                postId
        ).stream()
                .filter(tag -> tag != null && !tag.isEmpty())
                .toList();
    }

    private void loadTagsForPosts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        List<Long> ids = posts.stream().map(Post::getId).toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT post_id, tag FROM public.post_tags WHERE post_id IN (" + placeholders + ")";
        List<PostTagRow> rows = jdbcTemplate.query(
                sql,
                ids.toArray(),
                (rs, rowNum) -> new PostTagRow(
                        rs.getLong("post_id"),
                        rs.getString("tag")
                )
        );
        Map<Long, List<String>> tagsByPost = new HashMap<>();
        for (PostTagRow row : rows) {
            Long postId = row.postId();
            String tag = row.tag();
            if (tag != null) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    tagsByPost.computeIfAbsent(postId, k -> new ArrayList<>()).add(trimmed);
                }
            }
        }
        for (Post post : posts) {
            List<String> tags = tagsByPost.get(post.getId());
            post.setTags(tags != null ? tags : java.util.Collections.emptyList());
        }
    }

    private String buildWhereClauseBySearch(SearchCriteria criteria, List<Object> params) {
        List<String> conditions = new ArrayList<>();

        if (criteria.hasTextSubstring()) {
            conditions.add("(LOWER(p.title) LIKE LOWER(?) OR LOWER(p.\"text\") LIKE LOWER(?))");
            String like = "%" + criteria.getTextSubstring() + "%";
            params.add(like);
            params.add(like);
        }

        if (criteria.hasTags()) {
            for (String tag : criteria.getTags()) {
                conditions.add("EXISTS (SELECT 1 FROM public.post_tags pt WHERE pt.post_id = p.id AND LOWER(pt.tag) = LOWER(?))");
                params.add(tag);
            }
        }

        if (conditions.isEmpty()) {
            return "";
        }

        return " WHERE " + String.join(" AND ", conditions);
    }

    private record PostTagRow(long postId, String tag) {}
}