package io.github.levldev.blog.it.dao;

import io.github.levldev.blog.dao.PostDao;
import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.service.dto.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JdbcPostDaoIT {

    @Autowired JdbcClient jdbcClient;
    @Autowired
    PostDao postDao;

    @BeforeEach
    void cleanDb() {
        jdbcClient.sql("DELETE FROM public.post_tags").update();
        jdbcClient.sql("DELETE FROM public.comments").update();
        jdbcClient.sql("DELETE FROM public.posts").update();
    }

    @Test
    void create_then_findById_roundtrip() {
        Post p = new Post();
        p.setTitle("TestTitle1");
        p.setText("TestText1");
        p.setLikesCount(7);
        p.setTags(List.of("TagA", "TagB"));

        Post created = postDao.create(p);

        assertNotNull(created.getId());
        assertTrue(created.getId() > 0);

        List<String> dbTags = jdbcClient.sql("SELECT tag FROM public.post_tags WHERE post_id = :postId ORDER BY tag")
                .param("postId", created.getId())
                .query((rs, rowNum) -> rs.getString("tag"))
                .list();
        assertEquals(2, dbTags.size());
        assertTrue(dbTags.contains("TagA"));
        assertTrue(dbTags.contains("TagB"));

        Post loaded = postDao.findById(created.getId()).orElseThrow();

        assertEquals(created.getId(), loaded.getId());
        assertEquals("TestTitle1", loaded.getTitle());
        assertEquals("TestText1", loaded.getText());
        assertEquals(7, loaded.getLikesCount());

        assertNotNull(loaded.getTags());
        assertEquals(2, loaded.getTags().size());
        assertTrue(loaded.getTags().contains("TagA"));
        assertTrue(loaded.getTags().contains("TagB"));

        assertNotNull(loaded.getCreatedAt());
        assertNull(loaded.getImage());
        assertNull(loaded.getImageContentType());
    }

    @Test
    void update_post_roundtrip() {
        Post p = new Post();
        p.setTitle("TestTitle1");
        p.setText("TestText1");
        p.setLikesCount(5);
        p.setTags(List.of("TagA", "TagB"));

        Post created = postDao.create(p);
        long id = created.getId();

        Post toUpdate = postDao.findById(id).orElseThrow();
        toUpdate.setTitle("TestTitle2");
        toUpdate.setText("TestText2");
        toUpdate.setLikesCount(10);
        toUpdate.setTags(List.of("TagB", "TagC"));

        postDao.update(toUpdate);

        Post loaded = postDao.findById(id).orElseThrow();
        assertEquals("TestTitle2", loaded.getTitle());
        assertEquals("TestText2", loaded.getText());
        assertEquals(10, loaded.getLikesCount());
        assertNotNull(loaded.getTags());
        assertEquals(2, loaded.getTags().size());
        assertTrue(loaded.getTags().contains("TagB"));
        assertTrue(loaded.getTags().contains("TagC"));
        assertFalse(loaded.getTags().contains("TagA"));

        Long postTagsCount = jdbcClient.sql("SELECT COUNT(*) FROM public.post_tags WHERE post_id = :postId")
                .param("postId", id)
                .query((rs, rowNum) -> rs.getLong(1))
                .single();
        assertEquals(2L, postTagsCount);
        List<String> dbTags = jdbcClient.sql("SELECT tag FROM public.post_tags WHERE post_id = :postId ORDER BY tag")
                .param("postId", id)
                .query((rs, rowNum) -> rs.getString("tag"))
                .list();
        assertEquals(List.of("TagB", "TagC"), dbTags);
    }

    @Test
    void delete_post_cascades() {
        Post p = new Post();
        p.setTitle("TestTitle1");
        p.setText("TestText1");
        p.setTags(List.of("TagA", "TagB"));
        Post created = postDao.create(p);
        long postId = created.getId();

        jdbcClient.sql("INSERT INTO public.comments (post_id, \"text\") VALUES (:postId, :text)")
                .param("postId", postId)
                .param("text", "TestComment1")
                .update();

        postDao.deleteById(postId);

        assertTrue(postDao.findById(postId).isEmpty());
        Long tagRows = jdbcClient.sql("SELECT COUNT(*) FROM public.post_tags WHERE post_id = :postId")
                .param("postId", postId)
                .query((rs, rowNum) -> rs.getLong(1))
                .optional()
                .orElse(0L);
        assertEquals(0L, tagRows);
        Long commentRows = jdbcClient.sql("SELECT COUNT(*) FROM public.comments WHERE post_id = :postId")
                .param("postId", postId)
                .query((rs, rowNum) -> rs.getLong(1))
                .optional()
                .orElse(0L);
        assertEquals(0L, commentRows);
    }

    @Test
    void search_posts_textAndTag() {
        Post p1 = new Post();
        p1.setTitle("TestTitle1 MatchText");
        p1.setText("TestText1");
        p1.setTags(List.of("TagA"));
        postDao.create(p1);

        Post p2 = new Post();
        p2.setTitle("TestTitle2 MatchText");
        p2.setText("TestText2 MatchText");
        p2.setTags(List.of("TagC"));
        postDao.create(p2);

        Post p3 = new Post();
        p3.setTitle("TestTitle3 MatchText");
        p3.setText("TestText3 MatchText");
        p3.setTags(List.of("TagA"));
        postDao.create(p3);

        SearchCriteria criteria = SearchCriteria.builder().textSubstring("MatchText").tags(List.of("TagA")).build();
        long count = postDao.countBySearch(criteria);
        List<Post> page = postDao.findPage(criteria, 1, 10);

        assertEquals(2, count);
        assertEquals(2, page.size());
        List<Long> ids = page.stream().map(Post::getId).toList();
        assertTrue(ids.contains(p1.getId()));
        assertTrue(ids.contains(p3.getId()));
        assertFalse(ids.contains(p2.getId()));
    }
}