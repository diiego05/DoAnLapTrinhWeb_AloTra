package com.alotra.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "OrderItemToppings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemTopping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderItemId", nullable = false)
    private OrderItem orderItem;

    @Column(name = "ToppingId", nullable = false)
    private Long toppingId;

    @Column(name = "ToppingName", nullable = false)
    private String toppingName;

    @Column(name = "PriceAtAddition", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAddition;
}
