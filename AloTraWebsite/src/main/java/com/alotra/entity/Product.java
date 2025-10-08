package com.alotra.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@Data // Tự động tạo Getters, Setters, toString(), equals(), hashCode()
@Entity
@Table(name = "Products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "Slug", nullable = false, unique = true)
    private String slug;

    @Lob
    @Column(columnDefinition = "nvarchar(max)")
    private String description;

    @Column(name = "Status") // Trường status đã được thêm lại
    private String status;

    // Mối quan hệ: Nhiều sản phẩm thuộc về một danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryId")
    @ToString.Exclude // Loại trừ khỏi phương thức toString() để tránh vòng lặp
    @EqualsAndHashCode.Exclude // Tương tự
    private Category category;

    // Mối quan hệ: Một sản phẩm có nhiều media
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<ProductMedia> media = new HashSet<>();

    // THAY ĐỔI Ở ĐÂY: từ List thành Set
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<ProductVariant> variants = new HashSet<>();
}