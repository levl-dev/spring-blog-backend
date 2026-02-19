package io.github.levldev.blog.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {
    private List<String> tags;
    private String textSubstring;

    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }

    public boolean hasTextSubstring() {
        return textSubstring != null && !textSubstring.isBlank();
    }

    public boolean isEmpty() {
        return !hasTags() && !hasTextSubstring();
    }
}
