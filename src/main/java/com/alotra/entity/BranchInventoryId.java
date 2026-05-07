package com.alotra.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchInventoryId implements Serializable {
    private Long branchId;
    private Long variantId;
}
