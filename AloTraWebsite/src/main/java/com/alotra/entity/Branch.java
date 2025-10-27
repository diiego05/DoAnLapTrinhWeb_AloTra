package com.alotra.entity;

// CÁC IMPORT CẦN THIẾT CHO VALIDATION
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// CÁC IMPORT CHO JPA
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// IMPORT CHO LOMBOK
import lombok.Data;

@Data
@Entity
@Table(name = "Branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @NotBlank(message = "Tên chi nhánh không được để trống")
    @Size(min = 5, message = "Tên chi nhánh phải có ít nhất 5 ký tự")
    @Column(name = "Name", nullable = false, length = 200)
    private String name;

    @NotBlank(message = "Slug không được để trống")
    @Column(name = "Slug", nullable = false, unique = true, length = 220)
    private String slug;

    @NotBlank(message = "Địa chỉ không được để trống") // Thêm validation cho địa chỉ
    @Column(name = "Address", nullable = false, length = 500)
    private String address;

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    // 🆕 Tọa độ địa lý
    @Column(name = "Latitude")
    private Double latitude;

    @Column(name = "Longitude")
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ManagerId")
    private User manager;
}