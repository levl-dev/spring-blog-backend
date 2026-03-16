package io.github.levldev.blog.service.dto;

import io.github.levldev.blog.model.Post;

import java.util.List;

public record PostsPage(List<Post> posts, boolean hasPrev, boolean hasNext, int lastPage) {
}
