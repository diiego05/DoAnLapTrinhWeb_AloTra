package com.alotra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorProductSummaryDTO {
    private Long id;
    private String slug;
    private String name;
    private String imageUrl;
    private String priceRange;
    private Double lowestPrice;
    private Long defaultVariantId;
    private boolean hasDiscount;
    private Double originalPrice;
    private Double discountedPrice;
    private int discountPercent;
    private String status;                // ✅ trạng thái tổng của sản phẩm
    private String branchInventoryStatus; // ✅ trạng thái tại chi nhánh của vendor
}
