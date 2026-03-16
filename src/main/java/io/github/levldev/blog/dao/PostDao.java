package io.github.levldev.blog.dao;

import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.service.dto.ImageData;
import io.github.levldev.blog.service.dto.SearchCriteria;

import java.util.List;
import java.util.Optional;

public interface PostDao {

    Post create(Post post);

    Optional<Post> findById(long id);

    List<Post> findPage(SearchCriteria criteria, int pageNumber, int pageSize);

    long countBySearch(SearchCriteria criteria);

    Post update(Post post);

    void deleteById(long id);

    int incrementLikes(long id);

    void updateImage(long id, byte[] imageBytes, String imageContentType);

    ImageData getImage(long id);
}

