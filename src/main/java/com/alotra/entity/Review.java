package com.alotra.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = "orderItemId")
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int rating; // 1–5

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(nullable = false)
    private String status = "VISIBLE";

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewMedia> mediaList;
}
