package com.alotra.dto;

import java.time.LocalDateTime;

import lombok.Data;
@Data
public class ShipperDTO {
    private Long id;
    private String status;

    // 🧍 User info
    private String requesterName;
    private String requesterEmail;
    private String requesterPhone;
    private String idCardNumber;
    private String gender;
    private String dob;

    // 📍 Location
    private String ward;
    private String district;
    private String city;

    // 🚚 Vehicle info
    private String vehicleType;
    private String vehiclePlate;

    // 🏢 Carrier info
    private Long carrierId;
    private String carrierName;

    // 📜 System info
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
