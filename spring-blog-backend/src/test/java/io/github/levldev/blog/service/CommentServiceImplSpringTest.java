package io.github.levldev.blog.service;

import io.github.levldev.blog.config.TestConfig;
import io.github.levldev.blog.dao.CommentDao;
import io.github.levldev.blog.dao.PostDao;
import io.github.levldev.blog.model.Comment;
import io.github.levldev.blog.model.Post;
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
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class CommentServiceImplSpringTest {

    @Autowired
    private CommentServiceImpl commentService;

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private PostDao postDao;

    @BeforeEach
    void resetMocks() {
        reset(commentDao, postDao);
    }

    @Test
    void getCommentsForPost_returnsComments() {
        long postId = 1L;

        when(postDao.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(commentDao.findByPostId(postId)).thenReturn(List.of(new Comment(10L, postId, "first", null)));

        List<Comment> result = commentService.getCommentsForPost(postId);

        assertEquals(1, result.size());
        assertEquals("first", result.get(0).getText());
    }

    @Test
    void getCommentsForPost_whenPostNotFound_throws() {
        long postId = 404L;
        when(postDao.findById(postId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.getCommentsForPost(postId));

        verifyNoInteractions(commentDao);
    }

    @Test
    void getComment_returnsComment_whenBelongsToPost() {
        long postId = 1L;
        long commentId = 10L;

        when(postDao.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(commentDao.findById(commentId)).thenReturn(Optional.of(new Comment(commentId, postId, "hello", null)));

        Comment result = commentService.getComment(postId, commentId);

        assertEquals(commentId, result.getId());
        assertEquals(postId, result.getPostId());
        assertEquals("hello", result.getText());
    }

    @Test
    void getComment_whenCommentNotFound_throws() {
        long postId = 1L;
        long commentId = 999L;

        when(postDao.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(commentDao.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.getComment(postId, commentId));
    }

    @Test
    void getComment_whenCommentBelongsToOtherPost_throws() {
        long postId = 1L;
        long commentId = 10L;

        when(postDao.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(commentDao.findById(commentId)).thenReturn(Optional.of(new Comment(commentId, 999L, "other", null)));

        assertThrows(ResourceNotFoundException.class, () -> commentService.getComment(postId, commentId));
    }

    @Test
    void addComment_createsComment() {
        long postId = 1L;

        when(postDao.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(commentDao.create(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // вернём то, что передали

        Comment result = commentService.addComment(postId, "new comment");

        assertEquals(postId, result.getPostId());
        assertEquals("new comment", result.getText());
        verify(commentDao).create(any(Comment.class));
    }

    @Test
    void addComment_whenPostNotFound_throws() {
        long postId = 404L;
        when(postDao.findById(postId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.addComment(postId, "text"));

        verifyNoInteractions(commentDao);
    }

    @Test
    void updateComment_updatesText() {
        long postId = 1L;
        long commentId = 10L;

        when(postDao.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(commentDao.findById(commentId)).thenReturn(Optional.of(new Comment(commentId, postId, "old", null)));
        when(commentDao.update(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentService.updateComment(postId, commentId, "updated");

        assertEquals("updated", result.getText());
    }

    @Test
    void updateComment_whenCommentNotFound_throws() {
        long postId = 1L;
        long commentId = 999L;

        when(postDao.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(commentDao.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.updateComment(postId, commentId, "updated"));

        verify(commentDao, never()).update(any());
    }

    @Test
    void deleteComment_deletesWhenExists() {
        long postId = 1L;
        long commentId = 10L;

        when(postDao.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(commentDao.findById(commentId)).thenReturn(Optional.of(new Comment(commentId, postId, "x", null)));

        commentService.deleteComment(postId, commentId);

        verify(commentDao).deleteById(commentId);
    }

    @Test
    void deleteComment_whenCommentNotFound_throws() {
        long postId = 1L;
        long commentId = 999L;

        when(postDao.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(commentDao.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.deleteComment(postId, commentId));

        verify(commentDao, never()).deleteById(anyLong());
    }

    private static Post post(long id) {
        Post p = new Post();
        p.setId(id);
        return p;
    }
}