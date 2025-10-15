// com/alotra/entity/Order.java
package com.alotra.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "Orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="Code", nullable=false, length=30)
    private String code;

    @Column(name="UserId", nullable=false)
    private Long userId;

    @Column(name="BranchId")
    private Long branchId;

    @Column(name="ShippingCarrierId")
    private Long shippingCarrierId;

    @Column(name="CouponId")
    private Long couponId;

    @Column(name="DeliveryAddress", length=255)
    private String deliveryAddress; // snapshot chuỗi địa chỉ để “đóng băng” lúc đặt

    @Column(name="PaymentMethod", nullable=false, length=50)
    private String paymentMethod;

    @Column(name="Subtotal", nullable=false, precision=18, scale=2)
    private BigDecimal subtotal;

    @Column(name="ShippingFee", precision=18, scale=2)
    private BigDecimal shippingFee;

    @Column(name="Discount", precision=18, scale=2)
    private BigDecimal discount;

    @Column(name="Total", nullable=false, precision=18, scale=2)
    private BigDecimal total;

    @Column(name="Status", nullable=false, length=50)
    private String status;

    @Column(name="CreatedAt", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="UpdatedAt", nullable=false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;
}
