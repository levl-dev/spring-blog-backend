package io.github.levldev.blog.web.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostsPageResponse {
    private List<PostResponse> posts;
    private boolean hasPrev;
    private boolean hasNext;
    private int lastPage;
}
