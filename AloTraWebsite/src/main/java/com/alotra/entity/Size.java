// 📁 com/alotra/entity/Size.java
package com.alotra.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Sizes")
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    // --- THÊM THUỘC TÍNH MỚI ---
    @Column(name = "Code", unique = true)
    private String code;

    @Column(name = "Name", nullable = false, unique = true)
    private String name;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // --- THÊM GETTER & SETTER CHO CODE ---
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}