package com.alotra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "Sizes")
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên kích thước không được để trống")
    @Column(name = "Name", nullable = false, unique = true)
    private String name;

    // Đã xóa @NotBlank ở đây
    @Column(name = "Code", nullable = false, unique = true)
    private String code;
}