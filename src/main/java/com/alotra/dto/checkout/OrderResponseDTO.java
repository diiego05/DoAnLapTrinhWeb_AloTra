// com/alotra/dto/checkout/OrderResponseDTO.java
package com.alotra.dto.checkout;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponseDTO {
    private Long orderId;
    private String code;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private BigDecimal total;
    private List<OrderedLineDTO> items;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderedLineDTO {
        private String productName;
        private String sizeName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal toppingTotal;
        private BigDecimal lineTotal;
        private String note;
    }
}
