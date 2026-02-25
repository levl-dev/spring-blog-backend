package io.github.levldev.blog.config;

import io.github.levldev.blog.dao.CommentDao;
import io.github.levldev.blog.dao.JdbcCommentDao;
import io.github.levldev.blog.dao.JdbcPostDao;
import io.github.levldev.blog.dao.PostDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class H2DaoTestConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource ds) {
        var populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema-h2.sql"));

        var initializer = new DataSourceInitializer();
        initializer.setDataSource(ds);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    @Bean
    public PostDao postDao(JdbcTemplate jdbcTemplate) {
        return new JdbcPostDao(jdbcTemplate);
    }

    @Bean
    public CommentDao commentDao(JdbcTemplate jdbcTemplate) {
        return new JdbcCommentDao(jdbcTemplate);
    }
}