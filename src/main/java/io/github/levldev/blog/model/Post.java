package io.github.levldev.blog.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Post {

    @EqualsAndHashCode.Include
    private Long id;
    private String title;
    private String text;
    private LocalDateTime createdAt;
    private List<String> tags;
    private int likesCount;
    private int commentsCount;
    private byte[] image;
    private String imageContentType;
}