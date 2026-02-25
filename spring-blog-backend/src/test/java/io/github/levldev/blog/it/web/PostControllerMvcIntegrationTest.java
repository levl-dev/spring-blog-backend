package io.github.levldev.blog.it.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.levldev.blog.config.H2MvcTestConfig;
import io.github.levldev.blog.dao.PostDao;
import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.web.responses.PostResponse;
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
class PostControllerMvcIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PostDao postDao;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        jdbcTemplate.update("DELETE FROM public.post_tags");
        jdbcTemplate.update("DELETE FROM public.comments");
        jdbcTemplate.update("DELETE FROM public.posts");
    }

    @Test
    void createPost_returns201_andPersists() throws Exception {
        String json = """
                {"title":"TestTitle1","text":"TestText1","tags":["TagA","TagB"]}
                """;

        MvcResult result = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        PostResponse response = new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                PostResponse.class
        );
        assertNotNull(response.getId());
        assertEquals("TestTitle1", response.getTitle());
        assertEquals("TestText1", response.getText());
        assertNotNull(response.getTags());
        assertEquals(2, response.getTags().size());
        assertTrue(response.getTags().contains("TagA"));
        assertTrue(response.getTags().contains("TagB"));

        Long id = response.getId();

        Long postCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.posts WHERE id = ?", Long.class, id);
        assertEquals(1L, postCount);

        List<String> dbTags = jdbcTemplate.query(
                "SELECT tag FROM public.post_tags WHERE post_id = ? ORDER BY tag",
                (rs, rowNum) -> rs.getString("tag"),
                id
        );
        assertEquals(2, dbTags.size());
        assertTrue(dbTags.contains("TagA"));
        assertTrue(dbTags.contains("TagB"));
    }

    @Test
    void getPost_returns200_withFullPost() throws Exception {
        Post p = new Post();
        p.setTitle("TestTitle1");
        p.setText("TestText1 FullContent");
        p.setLikesCount(0);
        p.setCommentsCount(0);
        p.setTags(List.of("TagA", "TagB"));
        Post created = postDao.create(p);
        long id = created.getId();

        MvcResult result = mockMvc.perform(get("/api/posts/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        PostResponse response = new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                PostResponse.class
        );
        assertEquals(id, response.getId());
        assertEquals("TestTitle1", response.getTitle());
        assertEquals("TestText1 FullContent", response.getText());
        assertNotNull(response.getTags());
        assertEquals(2, response.getTags().size());
        assertTrue(response.getTags().contains("TagA"));
        assertTrue(response.getTags().contains("TagB"));
        assertEquals(0, response.getLikesCount());
        assertEquals(0, response.getCommentsCount());
    }

    @Test
    void updatePost_returns200_andReplacesTags() throws Exception {
        Post p = new Post();
        p.setTitle("TestTitle1");
        p.setText("TestText1");
        p.setLikesCount(0);
        p.setCommentsCount(0);
        p.setTags(List.of("TagA", "TagB"));
        Post created = postDao.create(p);
        long id = created.getId();

        String json = """
                {"title":"TestTitle2","text":"TestText2","tags":["TagB","TagC"]}
                """;

        MvcResult result = mockMvc.perform(put("/api/posts/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        PostResponse response = new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                PostResponse.class
        );
        assertEquals(id, response.getId());
        assertEquals("TestTitle2", response.getTitle());
        assertEquals("TestText2", response.getText());
        assertNotNull(response.getTags());
        assertEquals(2, response.getTags().size());
        assertTrue(response.getTags().contains("TagB"));
        assertTrue(response.getTags().contains("TagC"));
        assertFalse(response.getTags().contains("TagA"));

        List<String> dbTags = jdbcTemplate.query(
                "SELECT tag FROM public.post_tags WHERE post_id = ? ORDER BY tag",
                (rs, rowNum) -> rs.getString("tag"),
                id
        );
        assertEquals(List.of("TagB", "TagC"), dbTags);

        MvcResult getResult = mockMvc.perform(get("/api/posts/" + id))
                .andExpect(status().isOk())
                .andReturn();
        PostResponse getResponse = new ObjectMapper().readValue(
                getResult.getResponse().getContentAsString(),
                PostResponse.class
        );
        assertEquals("TestTitle2", getResponse.getTitle());
        assertNotNull(getResponse.getTags());
        assertEquals(2, getResponse.getTags().size());
        assertTrue(getResponse.getTags().contains("TagB"));
        assertTrue(getResponse.getTags().contains("TagC"));
    }

    @Test
    void deletePost_returns200_andCascadesCleanup() throws Exception {
        Post p = new Post();
        p.setTitle("TestTitle1");
        p.setText("TestText1");
        p.setLikesCount(0);
        p.setCommentsCount(0);
        p.setTags(List.of("TagA", "TagB"));
        Post created = postDao.create(p);
        long id = created.getId();

        jdbcTemplate.update("INSERT INTO public.comments (post_id, \"text\") VALUES (?, ?)", id, "TestComment1");

        mockMvc.perform(delete("/api/posts/" + id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + id))
                .andExpect(status().isNotFound());

        Long postCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.posts WHERE id = ?", Long.class, id);
        assertEquals(0L, postCount);

        Long tagCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.post_tags WHERE post_id = ?", Long.class, id);
        assertEquals(0L, tagCount);

        Long commentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.comments WHERE post_id = ?", Long.class, id);
        assertEquals(0L, commentCount);
    }
}
