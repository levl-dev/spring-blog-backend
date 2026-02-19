package io.github.levldev.blog.service;

import io.github.levldev.blog.model.Comment;

import java.util.List;

public interface CommentService {

    List<Comment> getCommentsForPost(long postId);

    Comment getComment(long postId, long commentId);

    Comment addComment(long postId, String text);

    Comment updateComment(long postId, long commentId, String text);

    void deleteComment(long postId, long commentId);
}