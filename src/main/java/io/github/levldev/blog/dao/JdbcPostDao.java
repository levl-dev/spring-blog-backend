package io.github.levldev.blog.dao;

import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.service.dto.ImageData;
import io.github.levldev.blog.service.dto.SearchCriteria;
import org.springframework.jdbc.core.JdbcTemplate;
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
public class JdbcPostDao implements PostDao {

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public JdbcPostDao(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Post create(Post post) {
        String sql = "INSERT INTO public.posts (title, \"text\", likes_count) VALUES (:title, :text, :likesCount)";

        KeyHolder kh = new GeneratedKeyHolder();

        jdbcClient.sql(sql)
                .param("title", post.getTitle())
                .param("text", post.getText())
                .param("likesCount", post.getLikesCount())
                .update(kh);

        var keys = kh.getKeys();
        if (keys == null) throw new IllegalStateException("No generated id returned");

        Long id = ((Number) keys.get("id")).longValue();

        post.setId(id);
        replaceTagsForPost(id, post.getTags());
        return post;
    }

    @Override
    public Optional<Post> findById(long id) {
        String sql = "SELECT id, title, \"text\", created_at, likes_count, image, image_content_type " +
                "FROM public.posts WHERE id = :id";
        Optional<Post> postOptional = jdbcClient.sql(sql)
                .param("id", id)
                .query((rs, rowNum) -> {
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
                })
                .optional();
        if (postOptional.isPresent()) {
            Post post = postOptional.get();
            post.setTags(findTagsForPost(post.getId()));
        }
        return postOptional;
    }

    @Override
    public List<Post> findPage(SearchCriteria criteria, int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;

        StringBuilder from = new StringBuilder(" FROM public.posts p");
        Map<String, Object> params = new HashMap<>();

        String where = buildWhereClauseBySearch(criteria, params);

        String sql = "SELECT p.id, p.title, p.\"text\", p.created_at, p.likes_count " +
                from + where +
                " ORDER BY p.id DESC " +
                "LIMIT :pageSize OFFSET :offset";

        params.put("pageSize", pageSize);
        params.put("offset", offset);

        List<Post> posts = jdbcClient.sql(sql)
                .params(params)
                .query((rs, rowNum) -> {
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
                })
                .list();
        loadTagsForPosts(posts);
        return posts;
    }

    @Override
    public long countBySearch(SearchCriteria criteria) {
        String from = " FROM public.posts p";
        Map<String, Object> params = new HashMap<>();

        String where = buildWhereClauseBySearch(criteria, params);

        String sql = "SELECT COUNT(*)" + from + where;

        return jdbcClient.sql(sql)
                .params(params)
                .query((rs, rowNum) -> rs.getLong(1))
                .optional()
                .orElse(0L);
    }

    @Override
    public Post update(Post post) {
        String sql = "UPDATE public.posts SET title = :title, \"text\" = :text, likes_count = :likesCount " +
                "WHERE id = :id";
        jdbcClient.sql(sql)
                .param("title", post.getTitle())
                .param("text", post.getText())
                .param("likesCount", post.getLikesCount())
                .param("id", post.getId())
                .update();
        replaceTagsForPost(post.getId(), post.getTags());
        return post;
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM public.posts WHERE id = :id";
        jdbcClient.sql(sql).param("id", id).update();
    }

    @Override
    public int incrementLikes(long id) {
        String updateSql = "UPDATE public.posts SET likes_count = likes_count + 1 WHERE id = :id";
        jdbcClient.sql(updateSql).param("id", id).update();
        String selectSql = "SELECT likes_count FROM public.posts WHERE id = :id";
        Integer likes = jdbcClient.sql(selectSql).param("id", id).query((rs, rowNum) -> rs.getInt("likes_count")).single();
        return likes;
    }

    @Override
    public void updateImage(long id, byte[] imageBytes, String imageContentType) {
        String sql = "UPDATE public.posts SET image = :image, image_content_type = :imageContentType WHERE id = :id";
        jdbcClient.sql(sql)
                .param("image", imageBytes)
                .param("imageContentType", imageContentType)
                .param("id", id)
                .update();
    }

    @Override
    public ImageData getImage(long id) {
        String sql = "SELECT image, image_content_type FROM public.posts WHERE id = :id";
        return jdbcClient.sql(sql)
                .param("id", id)
                .query((rs, rowNum) -> {
                    byte[] imageBytes = rs.getBytes("image");
                    String contentType = rs.getString("image_content_type");
                    return new ImageData(imageBytes, contentType);
                })
                .optional()
                .orElse(null);
    }

    private void replaceTagsForPost(long postId, List<String> tags) {
        jdbcClient.sql("DELETE FROM public.post_tags WHERE post_id = :postId").param("postId", postId).update();
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
        return jdbcClient.sql("SELECT tag FROM public.post_tags WHERE post_id = :postId ORDER BY tag")
                .param("postId", postId)
                .query((rs, rowNum) -> {
                    String tag = rs.getString("tag");
                    return tag != null ? tag.trim() : null;
                })
                .list()
                .stream()
                .filter(tag -> tag != null && !tag.isEmpty())
                .toList();
    }

    private void loadTagsForPosts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        List<Long> ids = posts.stream().map(Post::getId).toList();
        Map<String, Object> params = new HashMap<>();
        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            String key = "id" + i;
            placeholders.add(":" + key);
            params.put(key, ids.get(i));
        }
        String sql = "SELECT post_id, tag FROM public.post_tags WHERE post_id IN (" +
                String.join(",", placeholders) + ")";
        List<PostTagRow> rows = jdbcClient.sql(sql)
                .params(params)
                .query((rs, rowNum) -> new PostTagRow(
                        rs.getLong("post_id"),
                        rs.getString("tag")
                ))
                .list();
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
            List<String> postTags = tagsByPost.get(post.getId());
            post.setTags(postTags != null ? postTags : List.of());
        }
    }

    private String buildWhereClauseBySearch(SearchCriteria criteria, Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();

        if (criteria.hasTextSubstring()) {
            conditions.add("(LOWER(p.title) LIKE LOWER(:textLike1) OR LOWER(p.\"text\") LIKE LOWER(:textLike2))");
            String like = "%" + criteria.getTextSubstring() + "%";
            params.put("textLike1", like);
            params.put("textLike2", like);
        }

        if (criteria.hasTags()) {
            int i = 0;
            for (String tag : criteria.getTags()) {
                String paramName = "tag" + i++;
                conditions.add("EXISTS (SELECT 1 FROM public.post_tags pt WHERE pt.post_id = p.id AND LOWER(pt.tag) = LOWER(:" + paramName + "))");
                params.put(paramName, tag);
            }
        }

        if (conditions.isEmpty()) {
            return "";
        }

        return " WHERE " + String.join(" AND ", conditions);
    }

    private record PostTagRow(long postId, String tag) {}
}
