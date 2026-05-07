package com.alotra.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BranchInventory")
@IdClass(BranchInventoryId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchInventory {

    @Id
    @Column(name = "BranchId")
    private Long branchId;

    @Id
    @Column(name = "VariantId")
    private Long variantId;

    @Column(name = "Status", nullable = false)
    private String status;
}
