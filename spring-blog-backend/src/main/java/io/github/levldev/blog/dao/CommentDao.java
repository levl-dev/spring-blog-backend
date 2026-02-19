package io.github.levldev.blog.dao;

import io.github.levldev.blog.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentDao {

    Comment create(Comment comment);

    Optional<Comment> findById(long id);

    List<Comment> findByPostId(long postId);

    Comment update(Comment comment);

    void deleteById(long id);

    void deleteByPostId(long postId);

    long countByPostId(long postId);
}