// com/alotra/entity/OrderItem.java
package com.alotra.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "OrderItems")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="OrderId", nullable=false)
    private Order order;

    @Column(name="ProductId", nullable=false)
    private Long productId;

    @Column(name="VariantId")
    private Long variantId;

    @Column(name="ProductName", nullable=false, length=255)
    private String productName;   // snapshot

    @Column(name="SizeName", length=100)
    private String sizeName;      // snapshot

    @Column(name="Note", length=255)
    private String note;          // snapshot đá/ngọt…

    @Column(name="Quantity", nullable=false)
    private Integer quantity;

    @Column(name="UnitPrice", nullable=false, precision=18, scale=2)
    private BigDecimal unitPrice;       // giá variant tại thời điểm đặt

    @Column(name="ToppingTotal", precision=18, scale=2)
    private BigDecimal toppingTotal;    // tổng topping cho 1 đơn vị

    @Column(name="LineTotal", nullable=false, precision=18, scale=2)
    private BigDecimal lineTotal;       // (unitPrice + toppingTotal) * quantity
}
