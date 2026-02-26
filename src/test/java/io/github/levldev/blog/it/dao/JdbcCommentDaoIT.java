package io.github.levldev.blog.it.dao;

import io.github.levldev.blog.config.H2DaoTestConfig;
import io.github.levldev.blog.dao.CommentDao;
import io.github.levldev.blog.dao.PostDao;
import io.github.levldev.blog.model.Comment;
import io.github.levldev.blog.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = H2DaoTestConfig.class)
class JdbcCommentDaoIT {

    @Autowired JdbcClient jdbcClient;
    @Autowired
    CommentDao commentDao;
    @Autowired
    PostDao postDao;

    @BeforeEach
    void cleanDb() {
        jdbcClient.sql("DELETE FROM public.post_tags").update();
        jdbcClient.sql("DELETE FROM public.comments").update();
        jdbcClient.sql("DELETE FROM public.posts").update();
    }

    @Test
    void add_comment_to_post() {
        Post p = new Post();
        p.setTitle("TestTitle1");
        p.setText("TestText1");
        p.setLikesCount(0);
        p.setTags(List.of());
        Post created = postDao.create(p);
        long postId = created.getId();

        Comment c = new Comment();
        c.setPostId(postId);
        c.setText("TestComment1");
        Comment saved = commentDao.create(c);

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
        assertEquals(postId, saved.getPostId());
        assertEquals("TestComment1", saved.getText());

        List<Comment> comments = commentDao.findByPostId(postId);
        assertEquals(1, comments.size());
        assertEquals(saved.getId(), comments.get(0).getId());
        assertEquals("TestComment1", comments.get(0).getText());
    }

    @Test
    void delete_comment() {
        Post p = new Post();
        p.setTitle("TestTitle1");
        p.setText("TestText1");
        p.setLikesCount(0);
        p.setTags(List.of());
        Post created = postDao.create(p);
        long postId = created.getId();

        Comment c = new Comment();
        c.setPostId(postId);
        c.setText("TestComment1");
        Comment saved = commentDao.create(c);
        long commentId = saved.getId();

        commentDao.deleteById(commentId);

        assertTrue(commentDao.findById(commentId).isEmpty());
        List<Comment> remaining = commentDao.findByPostId(postId);
        assertTrue(remaining.isEmpty());
    }

    @Test
    void delete_comment_otherCommentsRemain() {
        Post p = new Post();
        p.setTitle("TestTitle1");
        p.setText("TestText1");
        p.setLikesCount(0);
        p.setTags(List.of());
        Post created = postDao.create(p);
        long postId = created.getId();

        Comment c1 = new Comment();
        c1.setPostId(postId);
        c1.setText("TestComment1");
        commentDao.create(c1);

        Comment c2 = new Comment();
        c2.setPostId(postId);
        c2.setText("TestComment2");
        Comment saved2 = commentDao.create(c2);

        commentDao.deleteById(saved2.getId());

        List<Comment> remaining = commentDao.findByPostId(postId);
        assertEquals(1, remaining.size());
        assertEquals("TestComment1", remaining.get(0).getText());
    }
}
