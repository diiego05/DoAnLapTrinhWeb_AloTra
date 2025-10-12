package com.alotra.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistId implements Serializable {
    private Long userId;
    private Long productId;
}
