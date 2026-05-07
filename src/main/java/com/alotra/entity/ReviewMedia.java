package com.alotra.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ReviewMedia")
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewId", nullable = false)
    private Review review;

    @Column(nullable = false, length = 600)
    private String url;

    @Column(nullable = false, length = 20)
    private String mediaType; // IMAGE hoặc VIDEO
}
