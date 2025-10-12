package com.alotra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "Toppings")/**/
public class Topping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên topping không được để trống")
    @Column(name = "Name", nullable = false, unique = true) // << THÊM unique = true
    private String name;

    @DecimalMin(value = "0.0", message = "Giá phải là một số không âm")
    @Column(name = "Price", nullable = false)
    private BigDecimal price;

    @Column(name = "Status", nullable = false)
    private String status = "ACTIVE";
}