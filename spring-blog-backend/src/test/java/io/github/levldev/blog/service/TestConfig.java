package io.github.levldev.blog.service;

import io.github.levldev.blog.dao.CommentDao;
import io.github.levldev.blog.dao.PostDao;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {

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
}