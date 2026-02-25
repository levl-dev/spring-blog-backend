package io.github.levldev.blog.it.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.levldev.blog.config.H2MvcTestConfig;
import io.github.levldev.blog.dao.CommentDao;
import io.github.levldev.blog.dao.PostDao;
import io.github.levldev.blog.model.Comment;
import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.web.responses.CommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(H2MvcTestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = "test.mode=integration")
class CommentControllerMvcIT {

    @Autowired WebApplicationContext wac;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PostDao postDao;
    @Autowired CommentDao commentDao;
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        jdbcTemplate.update("DELETE FROM public.post_tags");
        jdbcTemplate.update("DELETE FROM public.comments");
        jdbcTemplate.update("DELETE FROM public.posts");
    }

    @Test
    void getComments_returns200_withCommentList() throws Exception {
        Post post = createPost();
        long postId = post.getId();
        Comment c1 = createComment(postId, "CommentText1");
        Comment c2 = createComment(postId, "CommentText2");

        MvcResult result = mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        List<CommentResponse> comments = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<CommentResponse>>() {}
        );
        assertEquals(2, comments.size());
        assertTrue(comments.stream().anyMatch(r -> r.getId().equals(c1.getId()) && "CommentText1".equals(r.getText()) && postId == r.getPostId()));
        assertTrue(comments.stream().anyMatch(r -> r.getId().equals(c2.getId()) && "CommentText2".equals(r.getText()) && postId == r.getPostId()));
    }

    @Test
    void updateComment_returns200_andPersists() throws Exception {
        Post post = createPost();
        long postId = post.getId();
        Comment comment = createComment(postId, "CommentText1");
        long commentId = comment.getId();

        String json = "{\"text\":\"UpdatedCommentText\"}";
        MvcResult result = mockMvc.perform(put("/api/posts/" + postId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        CommentResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), CommentResponse.class);
        assertEquals("UpdatedCommentText", response.getText());

        String dbText = jdbcTemplate.queryForObject("SELECT \"text\" FROM public.comments WHERE id = ?", String.class, commentId);
        assertEquals("UpdatedCommentText", dbText);
    }

    @Test
    void deleteComment_returns200_andRemovesFromDb() throws Exception {
        Post post = createPost();
        long postId = post.getId();
        Comment comment = createComment(postId, "CommentText1");
        long commentId = comment.getId();

        mockMvc.perform(delete("/api/posts/" + postId + "/comments/" + commentId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + postId + "/comments/" + commentId))
                .andExpect(status().isNotFound());

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.comments WHERE id = ?", Long.class, commentId);
        assertEquals(0L, count);
    }

    private Post createPost() {
        Post p = new Post();
        p.setTitle("TestPostTitle");
        p.setText("TestPostText");
        p.setLikesCount(0);
        p.setCommentsCount(0);
        p.setTags(List.of());
        return postDao.create(p);
    }

    private Comment createComment(long postId, String text) {
        Comment c = new Comment();
        c.setPostId(postId);
        c.setText(text);
        return commentDao.create(c);
    }
}
