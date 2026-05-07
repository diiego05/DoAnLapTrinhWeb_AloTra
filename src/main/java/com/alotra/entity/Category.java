package com.alotra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "Categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Column(name = "Name", nullable = false)
    private String name;

    @NotBlank(message = "Slug không được để trống")
    @Column(name = "Slug", unique = true, nullable = false)
    private String slug;

    @Column(name = "SortOrder")
    private int sortOrder = 0;

    // Mối quan hệ tự tham chiếu: Nhiều danh mục con thuộc về MỘT danh mục cha
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentId")
    private Category parent;

    // Một danh mục cha có thể có nhiều danh mục con
    @OneToMany(mappedBy = "parent")
    private List<Category> children;
}