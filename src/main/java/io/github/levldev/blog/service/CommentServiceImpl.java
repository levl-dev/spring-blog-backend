package io.github.levldev.blog.service;

import io.github.levldev.blog.dao.CommentDao;
import io.github.levldev.blog.dao.PostDao;
import io.github.levldev.blog.model.Comment;
import io.github.levldev.blog.web.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentDao commentDao;
    private final PostDao postDao;

    public CommentServiceImpl(CommentDao commentDao, PostDao postDao) {
        this.commentDao = commentDao;
        this.postDao = postDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getCommentsForPost(long postId) {
        assertPostExists(postId);
        return commentDao.findByPostId(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public Comment getComment(long postId, long commentId) {
        assertPostExists(postId);
        Comment comment = commentDao.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment with id " + commentId + " not found"));
        if (!postIdEquals(comment, postId)) {
            throw new ResourceNotFoundException("Comment with id " + commentId + " not found for post " + postId);
        }
        return comment;
    }

    @Override
    public Comment addComment(long postId, String text) {
        assertPostExists(postId);
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setText(text);
        return commentDao.create(comment);
    }

    @Override
    public Comment updateComment(long postId, long commentId, String text) {
        Comment existing = getComment(postId, commentId);
        existing.setText(text);
        return commentDao.update(existing);
    }

    @Override
    public void deleteComment(long postId, long commentId) {
        getComment(postId, commentId);
        commentDao.deleteById(commentId);
    }

    private void assertPostExists(long postId) {
        postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post with id " + postId + " not found"));
    }

    private boolean postIdEquals(Comment comment, long postId) {
        return comment.getPostId() != null && comment.getPostId() == postId;
    }
}