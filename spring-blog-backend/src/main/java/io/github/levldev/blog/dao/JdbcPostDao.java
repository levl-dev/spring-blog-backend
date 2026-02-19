package io.github.levldev.blog.dao;

import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.service.dto.ImageData;
import io.github.levldev.blog.service.dto.SearchCriteria;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPostDao implements PostDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPostDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Post create(Post post) {
        String sql = "INSERT INTO public.posts (title, \"text\", tags, likes_count) " +
                "VALUES (?, ?, ?, ?) RETURNING id";

        String[] tagsArray = post.getTags() != null
                ? post.getTags().toArray(new String[0])
                : new String[0];

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                post.getTitle(),
                post.getText(),
                tagsArray,
                post.getLikesCount()
        );

        post.setId(id);
        return post;
    }

    @Override
    public Optional<Post> findById(long id) {
        String sql = "SELECT id, title, \"text\", created_at, tags, likes_count, image, image_content_type " +
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
                Array tagsArray = rs.getArray("tags");
                p.setTags(parseTags(tagsArray));
                p.setLikesCount(rs.getInt("likes_count"));
                p.setImage(rs.getBytes("image"));
                p.setImageContentType(rs.getString("image_content_type"));
                return p;
            }, id);
            return Optional.ofNullable(post);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Post> findPage(SearchCriteria criteria, int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;

        StringBuilder where = new StringBuilder();
        List<String> conditions = new java.util.ArrayList<>();
        if (criteria.hasTextSubstring()) {
            conditions.add("(LOWER(title) LIKE LOWER(?) OR LOWER(\"text\") LIKE LOWER(?))");
        }
        if (criteria.hasTags()) {
            for (int i = 0; i < criteria.getTags().size(); i++) {
                conditions.add("EXISTS (SELECT 1 FROM unnest(tags) t WHERE LOWER(t) = LOWER(?))");
            }
        }
        if (!conditions.isEmpty()) {
            where.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        String sql = "SELECT id, title, \"text\", created_at, tags, likes_count " +
                "FROM public.posts " + where + " " +
                "ORDER BY id DESC " +
                "LIMIT ? OFFSET ?";

        PreparedStatementSetter pss = ps -> setSearchParams(ps, criteria, pageSize, offset);

        return jdbcTemplate.query(sql, pss, (rs, rowNum) -> {
            Post p = new Post();
            p.setId(rs.getLong("id"));
            p.setTitle(rs.getString("title"));
            p.setText(rs.getString("text"));
            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                p.setCreatedAt(created.toLocalDateTime());
            }
            Array tagsArray = rs.getArray("tags");
            p.setTags(parseTags(tagsArray));
            p.setLikesCount(rs.getInt("likes_count"));
            return p;
        });
    }

    @Override
    public long countBySearch(SearchCriteria criteria) {
        StringBuilder where = new StringBuilder();
        List<String> conditions = new java.util.ArrayList<>();
        if (criteria.hasTextSubstring()) {
            conditions.add("(LOWER(title) LIKE LOWER(?) OR LOWER(\"text\") LIKE LOWER(?))");
        }
        if (criteria.hasTags()) {
            for (int i = 0; i < criteria.getTags().size(); i++) {
                conditions.add("EXISTS (SELECT 1 FROM unnest(tags) t WHERE LOWER(t) = LOWER(?))");
            }
        }
        if (!conditions.isEmpty()) {
            where.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        String sql = "SELECT COUNT(*) FROM public.posts " + where;

        PreparedStatementSetter pss = ps -> setSearchParams(ps, criteria, -1, -1);

        Long count = jdbcTemplate.query(sql, pss, rs -> {
            if (rs.next()) return rs.getLong(1);
            return 0L;
        });
        return count != null ? count : 0L;
    }

    private void setSearchParams(PreparedStatement ps, SearchCriteria criteria, int pageSize, int offset) throws SQLException {
        int idx = 1;
        if (criteria.hasTextSubstring()) {
            String like = "%" + criteria.getTextSubstring() + "%";
            ps.setString(idx++, like);
            ps.setString(idx++, like);
        }
        if (criteria.hasTags()) {
            for (String tag : criteria.getTags()) {
                ps.setString(idx++, tag);
            }
        }
        if (pageSize >= 0) {
            ps.setInt(idx++, pageSize);
        }
        if (offset >= 0) {
            ps.setInt(idx++, offset);
        }
    }

    @Override
    public Post update(Post post) {
        String sql = "UPDATE public.posts SET title = ?, \"text\" = ?, tags = ?, likes_count = ? " +
                "WHERE id = ?";
        String[] tagsArray = post.getTags() != null
                ? post.getTags().toArray(new String[0])
                : new String[0];
        jdbcTemplate.update(sql,
                post.getTitle(),
                post.getText(),
                tagsArray,
                post.getLikesCount(),
                post.getId());
        return post;
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM public.posts WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public int incrementLikes(long id) {
        String sql = "UPDATE public.posts SET likes_count = likes_count + 1 WHERE id = ? RETURNING likes_count";
        Integer likes = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return likes != null ? likes : 0;
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

    private List<String> parseTags(Array tagsArray) {
        if (tagsArray == null) {
            return Collections.emptyList();
        }
        try {
            String[] values = (String[]) tagsArray.getArray();
            return Arrays.stream(values)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}