package com.alotra.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductListDTO {
    private Long id;
    private String primaryImageUrl;
    private String name;
    private String categoryName;
    private String status;

    // ✅ Thêm mới cho phần hiển thị giảm giá
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private boolean hasDiscount;
    private int discountPercent;
}
