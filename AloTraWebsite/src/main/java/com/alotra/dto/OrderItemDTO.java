package com.alotra.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private Long variantId;// 🟢 phải có
    private String productName;
    private String sizeName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal toppingTotal;
    private BigDecimal lineTotal;
    private String note;
    private List<ToppingDTO> toppings;
    private String deliveryAddress;

}
