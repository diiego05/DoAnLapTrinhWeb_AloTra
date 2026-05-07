package com.alotra.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipperRequestDTO {
    private Long carrierId;
    private String vehicleType;
    private String vehiclePlate;
    private String ward;
    private String district;
    private String city;
}
