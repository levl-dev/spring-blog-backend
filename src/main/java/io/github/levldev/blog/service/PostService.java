package io.github.levldev.blog.service;

import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.service.dto.PostsPage;
import io.github.levldev.blog.service.dto.ImageData;

import java.util.List;

public interface PostService {

    Post createPost(String title, String text, List<String> tags);

    Post getPost(long id);

    Post updatePost(long id, String title, String text, List<String> tags);

    void deletePost(long id);

    int incrementLikes(long id);

    void updateImage(long id, byte[] imageBytes, String imageContentType);

    ImageData getImage(long id);

    PostsPage getPostsPage(String search, int pageNumber, int pageSize);
}


