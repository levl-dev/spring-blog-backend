package io.github.levldev.blog.service.dto;

import io.github.levldev.blog.model.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostsPage {

    private List<Post> posts;
    private boolean hasPrev;
    private boolean hasNext;
    private int lastPage;
}

