package io.github.levldev.blog.service;

import io.github.levldev.blog.dao.CommentDao;
import io.github.levldev.blog.dao.PostDao;
import io.github.levldev.blog.model.Post;
import io.github.levldev.blog.service.dto.ImageData;
import io.github.levldev.blog.service.dto.PostsPage;
import io.github.levldev.blog.service.dto.SearchCriteria;
import io.github.levldev.blog.web.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private static final int TEXT_PREVIEW_LIMIT = 128;

    private final PostDao postDao;
    private final CommentDao commentDao;

    public PostServiceImpl(PostDao postDao, CommentDao commentDao) {
        this.postDao = postDao;
        this.commentDao = commentDao;
    }

    @Override
    public Post createPost(String title, String text, List<String> tags) {
        Post post = new Post();
        post.setTitle(title);
        post.setText(text);
        post.setTags(tags);
        post.setLikesCount(0);
        post.setCommentsCount(0);
        post.setImage(null);
        return postDao.create(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Post getPost(long id) {
        Post post = postDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post with id " + id + " not found"));
        int commentsCount = (int) commentDao.countByPostId(id);
        post.setCommentsCount(commentsCount);
        return post;
    }

    @Override
    public Post updatePost(long id, String title, String text, List<String> tags) {
        Post existing = getPost(id);
        existing.setTitle(title);
        existing.setText(text);
        existing.setTags(tags);
        return postDao.update(existing);
    }

    @Override
    public void deletePost(long id) {
        postDao.deleteById(id);
    }

    @Override
    public int incrementLikes(long id) {
        getPost(id);
        return postDao.incrementLikes(id);
    }

    @Override
    public void updateImage(long id, byte[] imageBytes, String imageContentType) {
        getPost(id);
        postDao.updateImage(id, imageBytes, imageContentType);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageData getImage(long id) {
        getPost(id);
        ImageData imageData = postDao.getImage(id);
        if (imageData == null || imageData.getBytes() == null) {
            throw new ResourceNotFoundException("Post with id " + id + " has no image");
        }
        return imageData;
    }

    @Override
    @Transactional(readOnly = true)
    public PostsPage getPostsPage(String search, int pageNumber, int pageSize) {
        SearchCriteria criteria = parseSearchQuery(search);

        long total = postDao.countBySearch(criteria);
        int lastPage = total == 0 ? 1 : (int) Math.ceil((double) total / pageSize);
        if (pageNumber < 1) {
            pageNumber = 1;
        }
        if (pageNumber > lastPage) {
            pageNumber = lastPage;
        }

        List<Post> posts = postDao.findPage(criteria, pageNumber, pageSize);
        for (Post p : posts) {
            int commentsCount = (int) commentDao.countByPostId(p.getId());
            p.setCommentsCount(commentsCount);

            String text = p.getText();
            if (text != null && text.length() > TEXT_PREVIEW_LIMIT) {
                p.setText(text.substring(0, TEXT_PREVIEW_LIMIT) + "…");
            }
        }

        boolean hasPrev = pageNumber > 1;
        boolean hasNext = pageNumber < lastPage;

        return new PostsPage(posts, hasPrev, hasNext, lastPage);
    }

    private SearchCriteria parseSearchQuery(String search) {
        if (search == null || search.isBlank()) {
            return SearchCriteria.builder().tags(List.of()).textSubstring(null).build();
        }

        List<String> tags = new ArrayList<>();
        List<String> textTerms = new ArrayList<>();

        for (String searchPart : search.trim().split("\\s+")) {
            if (searchPart.startsWith("#")) {
                String tagValue = searchPart.substring(1).trim();
                if (!tagValue.isEmpty()) {
                    tags.add(tagValue);
                }
            } else if (!searchPart.isEmpty()) {
                textTerms.add(searchPart);
            }
        }

        String textSubstring = textTerms.isEmpty()
                ? null
                : String.join(" ", textTerms);

        return SearchCriteria.builder()
                .tags(tags)
                .textSubstring(textSubstring)
                .build();
    }
}

