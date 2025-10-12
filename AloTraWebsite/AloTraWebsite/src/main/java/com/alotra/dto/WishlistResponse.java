package com.alotra.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String thumbnailUrl;
}
