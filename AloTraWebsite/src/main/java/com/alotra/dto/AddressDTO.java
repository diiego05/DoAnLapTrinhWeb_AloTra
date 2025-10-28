package com.alotra.dto;

import com.alotra.entity.Address;

public record AddressDTO(
        Long id,
        String recipient,
        String phone,
        String line1,
        String ward,
        String city,
        boolean isDefault,
        Double latitude,
        Double longitude
) {
    public static AddressDTO from(Address entity) {
        return new AddressDTO(
                entity.getId(),
                entity.getRecipient(),
                entity.getPhone(),
                entity.getLine1(),
                entity.getWard(),
                entity.getCity(),
                entity.isDefault(),
                entity.getLatitude(),
                entity.getLongitude()
        );
    }
}
