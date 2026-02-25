package io.github.levldev.blog.config;

import io.github.levldev.blog.dao.CommentDao;
import io.github.levldev.blog.dao.PostDao;
import io.github.levldev.blog.service.CommentServiceImpl;
import io.github.levldev.blog.service.PostServiceImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockServiceTestConfig {
    @Bean
    public PostDao postDao() {
        return Mockito.mock(PostDao.class);
    }

    @Bean
    public CommentDao commentDao() {
        return Mockito.mock(CommentDao.class);
    }

    @Bean
    public PostServiceImpl postService(PostDao postDao, CommentDao commentDao) {
        return new PostServiceImpl(postDao, commentDao);
    }

    @Bean
    public CommentServiceImpl commentService(CommentDao commentDao, PostDao postDao) {
        return new CommentServiceImpl(commentDao, postDao);
    }
}