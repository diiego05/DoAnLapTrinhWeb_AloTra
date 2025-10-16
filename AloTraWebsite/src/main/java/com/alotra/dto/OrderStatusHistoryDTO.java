package com.alotra.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatusHistoryDTO {
    private String status;
    private LocalDateTime changedAt;
    private String note;
}
