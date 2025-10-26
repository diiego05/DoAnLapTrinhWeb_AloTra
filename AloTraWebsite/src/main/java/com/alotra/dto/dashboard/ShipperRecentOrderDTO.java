package com.alotra.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ShipperRecentOrderDTO {
    private String code;
    private String customerName;
    private String address;
    private String status;
    private LocalDateTime createdAt;
}
