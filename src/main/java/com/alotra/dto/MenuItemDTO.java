package com.alotra.dto;

import com.alotra.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemDTO {
    private Long id;
    private String title;
    private String url;
    private String slug;
    private Integer sortOrder;
    private boolean hasChildren;
    private List<MenuItemDTO> children;

    public static MenuItemDTO from(Category category) {
        if (category == null) return null;

        return MenuItemDTO.builder()
                .id(category.getId())
                .title(category.getName())
                .url("/alotra-website/category/" + category.getSlug())
                .slug(category.getSlug())
                .sortOrder(category.getSortOrder())
                .hasChildren(category.getChildren() != null && !category.getChildren().isEmpty())
                .children(category.getChildren() != null
                        ? category.getChildren().stream()
                                .map(MenuItemDTO::from)
                                .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}