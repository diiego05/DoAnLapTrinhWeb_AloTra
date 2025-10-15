package com.alotra.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipperRegisterDTO {
    private Long carrierId;
    private String vehicleType;
    private String vehiclePlate;

    // 🆕 Thêm 3 trường khu vực hoạt động
    private String ward;
    private String district;
    private String city;
}
