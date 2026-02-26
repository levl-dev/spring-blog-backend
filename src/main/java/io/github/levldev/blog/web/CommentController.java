package io.github.levldev.blog.web;

import io.github.levldev.blog.model.Comment;
import io.github.levldev.blog.service.CommentService;
import io.github.levldev.blog.web.responses.CommentResponse;
import io.github.levldev.blog.web.requests.CreateCommentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/undefined/comments")
    public List<Comment> getUndefinedComment() { return Collections.emptyList(); }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable("postId") long postId) {
        List<Comment> comments = commentService.getCommentsForPost(postId);
        List<CommentResponse> response = comments.stream()
                .map(CommentResponse::fromComment)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable("postId") long postId, @PathVariable("commentId") long commentId) {
        Comment comment = commentService.getComment(postId, commentId);
        return ResponseEntity.ok(CommentResponse.fromComment(comment));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable("postId") long postId, @RequestBody CreateCommentRequest request) {
        Comment created = commentService.addComment(postId, request.getText());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.fromComment(created));
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable("postId") long postId, @PathVariable("commentId") long commentId, @RequestBody CreateCommentRequest request) {
        Comment updated = commentService.updateComment(postId, commentId, request.getText());
        return ResponseEntity.ok(CommentResponse.fromComment(updated));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable("postId") long postId, @PathVariable("commentId") long commentId) {
        commentService.deleteComment(postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
