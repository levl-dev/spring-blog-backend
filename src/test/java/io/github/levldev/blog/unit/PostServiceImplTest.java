package io.github.levldev.blog.unit;

import io.github.levldev.blog.config.MockServiceTestConfig;
import io.github.levldev.blog.dao.CommentDao;
import io.github.levldev.blog.dao.PostDao;
import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.service.PostServiceImpl;
import io.github.levldev.blog.service.dto.ImageData;
import io.github.levldev.blog.service.dto.PostsPage;
import io.github.levldev.blog.service.dto.SearchCriteria;
import io.github.levldev.blog.web.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MockServiceTestConfig.class)
class PostServiceImplTest {

    @Autowired
    private PostServiceImpl postService;

    @Autowired
    private PostDao postDao;

    @Autowired
    private CommentDao commentDao;

    @BeforeEach
    void resetMocks() {
        reset(postDao, commentDao);
    }

    @Test
    void getPost_whenExists_setsCommentsCount() {
        long id = 1L;
        Post post = new Post();
        post.setId(id);

        when(postDao.findById(id)).thenReturn(Optional.of(post));
        when(commentDao.countByPostId(id)).thenReturn(3L);

        Post result = postService.getPost(id);

        assertEquals(3, result.getCommentsCount());
        verify(postDao).findById(id);
        verify(commentDao).countByPostId(id);
    }

    @Test
    void getPost_whenNotFound_throws() {
        long id = 404L;

        when(postDao.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> postService.getPost(id));
        verify(postDao).findById(id);
        verifyNoInteractions(commentDao);
    }

    @Test
    void updatePost_updatesFieldsAndKeepsCounters() {
        long id = 1L;

        Post existing = new Post();
        existing.setId(id);
        existing.setTitle("Old");
        existing.setText("Old text");
        existing.setTags(List.of("x"));
        existing.setLikesCount(2);

        when(postDao.findById(id)).thenReturn(Optional.of(existing));
        when(postDao.update(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.updatePost(id, "New", "New text", List.of("y"));

        assertEquals("New", result.getTitle());
        assertEquals("New text", result.getText());
        assertEquals(List.of("y"), result.getTags());
        assertEquals(2, result.getLikesCount());
    }

    @Test
    void updatePost_whenNotFound_throws() {
        when(postDao.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> postService.updatePost(404L, "T", "B", List.of()));
        verify(postDao).findById(404L);
        verify(postDao, never()).update(any());
    }

    @Test
    void incrementLikes_whenExists_returnsNewCount() {
        long id = 1L;
        Post post = new Post();
        post.setId(id);
        when(postDao.findById(id)).thenReturn(Optional.of(post));
        when(postDao.incrementLikes(id)).thenReturn(6);

        int result = postService.incrementLikes(id);

        assertEquals(6, result);
        verify(postDao).findById(id);
        verify(postDao).incrementLikes(id);
    }

    @Test
    void incrementLikes_whenNotFound_throws() {
        when(postDao.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> postService.incrementLikes(404L));
        verify(postDao).findById(404L);
        verify(postDao, never()).incrementLikes(anyLong());
    }

    @Test
    void updateImage_whenExists_delegatesToDao() {
        long id = 1L;
        Post post = new Post();
        post.setId(id);
        when(postDao.findById(id)).thenReturn(Optional.of(post));

        postService.updateImage(id, new byte[]{1, 2, 3}, "image/png");

        verify(postDao).findById(id);
        verify(postDao).updateImage(id, new byte[]{1, 2, 3}, "image/png");
    }

    @Test
    void getImage_whenExists_returnsImageData() {
        long id = 1L;
        Post post = new Post();
        post.setId(id);
        ImageData data = new ImageData(new byte[]{1, 2}, "image/jpeg");
        when(postDao.findById(id)).thenReturn(Optional.of(post));
        when(postDao.getImage(id)).thenReturn(data);

        ImageData result = postService.getImage(id);

        assertNotNull(result);
        assertArrayEquals(new byte[]{1, 2}, result.bytes());
        assertEquals("image/jpeg", result.contentType());
        verify(postDao).findById(id);
        verify(postDao).getImage(id);
    }

    @Test
    void getImage_whenPostHasNoImage_throws() {
        long id = 1L;
        Post post = new Post();
        post.setId(id);
        when(postDao.findById(id)).thenReturn(Optional.of(post));
        when(postDao.getImage(id)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> postService.getImage(id));

        verify(postDao).findById(id);
        verify(postDao).getImage(id);
    }

    @Test
    void getPostsPage_emptySearch_returnsUnfilteredPage() {
        Post p1 = new Post();
        p1.setId(1L);
        p1.setText("short");
        when(postDao.countBySearch(argThat(SearchCriteria::isEmpty))).thenReturn(10L);
        when(postDao.findPage(argThat(SearchCriteria::isEmpty), eq(1), eq(5)))
                .thenReturn(List.of(p1));
        when(commentDao.countByPostIds(List.of(1L)))
                .thenReturn(java.util.Map.of(1L, 0));

        PostsPage page = postService.getPostsPage("", 1, 5);

        assertEquals(1, page.posts().size());
        assertFalse(page.hasPrev());
        assertTrue(page.hasNext());
        assertEquals(2, page.lastPage());
        verify(postDao).countBySearch(any(SearchCriteria.class));
        verify(postDao).findPage(any(SearchCriteria.class), eq(1), eq(5));
    }

    @Test
    void getPostsPage_longText_truncatesTo128Chars() {
        Post p = new Post();
        p.setId(1L);
        p.setText("a".repeat(200));
        when(postDao.countBySearch(any(SearchCriteria.class))).thenReturn(1L);
        when(postDao.findPage(any(SearchCriteria.class), eq(1), eq(5))).thenReturn(List.of(p));
        when(commentDao.countByPostIds(List.of(1L)))
                .thenReturn(java.util.Map.of(1L, 0));

        PostsPage page = postService.getPostsPage("foo", 1, 5);

        assertEquals(1, page.posts().size());
        String text = page.posts().get(0).getText();
        assertEquals(129, text.length());
        assertTrue(text.startsWith("aaa"));
        assertTrue(text.endsWith("…"));
    }
}