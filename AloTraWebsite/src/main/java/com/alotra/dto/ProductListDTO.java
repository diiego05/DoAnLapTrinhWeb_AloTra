package com.alotra.dto;

import lombok.Data;

/**
 * DTO (Data Transfer Object) for displaying products in a list format.
 * Contains only the essential information needed for the product list view.
 */
@Data
public class ProductListDTO {
    private Long id;
    private String primaryImageUrl;
    private String name;
    private String categoryName;
    private String status;
}