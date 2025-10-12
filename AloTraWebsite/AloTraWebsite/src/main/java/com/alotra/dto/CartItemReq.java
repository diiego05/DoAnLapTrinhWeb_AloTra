package com.alotra.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemReq {
    private Long productId;      // ID sản phẩm
    private Long categoryId;     // Danh mục của sản phẩm
    private Long branchId;       // Chi nhánh bán sản phẩm
    private BigDecimal unitPrice;// Giá gốc của sản phẩm (đã lấy từ ProductService)
    private Integer quantity;    // Số lượng mua
}
