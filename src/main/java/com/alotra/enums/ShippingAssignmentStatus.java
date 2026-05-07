package com.alotra.enums;

public enum ShippingAssignmentStatus {
	PENDING,      // Mới giao đến shipper
    ACCEPTED,     // Một shipper nhận đơn
    DELIVERED,    // Giao thành công
    FAILED        // Giao thất bại
}
