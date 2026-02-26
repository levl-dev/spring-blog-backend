package io.github.levldev.blog.web;

import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.service.dto.PostsPage;
import io.github.levldev.blog.service.PostService;
import io.github.levldev.blog.service.dto.ImageData;
import io.github.levldev.blog.web.requests.CreatePostRequest;
import io.github.levldev.blog.web.responses.PostResponse;
import io.github.levldev.blog.web.responses.PostsPageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<PostsPageResponse> getPosts(@RequestParam("search") String search, @RequestParam("pageNumber") int pageNumber, @RequestParam("pageSize") int pageSize) {
        PostsPage page = postService.getPostsPage(search, pageNumber, pageSize);
        List<PostResponse> posts = page.posts().stream()
                .map(PostResponse::fromPost)
                .toList();

        PostsPageResponse response = new PostsPageResponse(
                posts,
                page.hasPrev(),
                page.hasNext(),
                page.lastPage()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable("id") long id) {
        Post post = postService.getPost(id);
        return ResponseEntity.ok(PostResponse.fromPost(post));
    }

    @PostMapping("")
    public ResponseEntity<PostResponse> createPost(@RequestBody CreatePostRequest request) {
        Post post = postService.createPost(request.getTitle(), request.getText(), request.getTags());
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.fromPost(post));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable("id") long id, @RequestBody CreatePostRequest request) {
        Post updated = postService.updatePost(id, request.getTitle(), request.getText(), request.getTags());
        return ResponseEntity.ok(PostResponse.fromPost(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable("id") long id) {
        postService.deletePost(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<Integer> incrementLikes(@PathVariable("id") long id) {
        int newLikes = postService.incrementLikes(id);
        return ResponseEntity.ok(newLikes);
    }

    @PutMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateImage(@PathVariable("id") long id,
                                            @RequestParam("image") MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        byte[] bytes = image.getBytes();
        String contentType = image.getContentType();
        postService.updateImage(id, bytes, contentType);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("id") long id) {
        ImageData imageData = postService.getImage(id);
        
        if (imageData == null || imageData.bytes() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String contentType = imageData.contentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(imageData.bytes());
    }
}
