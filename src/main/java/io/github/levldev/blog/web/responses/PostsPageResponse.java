package io.github.levldev.blog.web.responses;

import java.util.List;

public record PostsPageResponse(List<PostResponse> posts, boolean hasPrev, boolean hasNext, int lastPage) {
}
