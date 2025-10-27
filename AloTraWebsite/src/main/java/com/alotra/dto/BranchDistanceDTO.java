package com.alotra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDistanceDTO {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String status;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
}
