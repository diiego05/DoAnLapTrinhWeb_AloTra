package com.alotra.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ShippingAssignments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ShippingAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long shipperId;

    @Column(nullable = false, length = 50)
    private String status; // PENDING, ACCEPTED, DELIVERED, FAILED

    private LocalDateTime assignedAt;
    private LocalDateTime deliveredAt;

    @Column(length = 500)
    private String note;
}
