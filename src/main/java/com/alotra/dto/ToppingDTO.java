package com.alotra.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ToppingDTO {
    private String toppingName;
    private BigDecimal priceAtAddition;
}
