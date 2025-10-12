package com.alotra.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Wishlists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(WishlistId.class)
public class Wishlist {

    @Id
    @Column(name = "UserId")
    private Long userId;

    @Id
    @Column(name = "ProductId")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", insertable = false, updatable = false)
    private Product product;

    public Wishlist(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }
}
