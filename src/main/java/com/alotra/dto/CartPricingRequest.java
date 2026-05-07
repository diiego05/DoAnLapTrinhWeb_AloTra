package com.alotra.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartPricingRequest {
    private List<CartItemReq> items;   // Danh sách sản phẩm trong giỏ
    private Long branchId;             // Chi nhánh bán
    private Long carrierId;            // ID đơn vị vận chuyển
    private String couponCode;         // Mã giảm giá (nếu có)
}
