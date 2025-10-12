package com.alotra.dto;

import com.alotra.entity.Address;

public record AddressDTO(
        Long id,
        String recipient,
        String phone,
        String line1,
        String ward,
        String district,
        String city,
        boolean isDefault
) {
    public static AddressDTO from(Address entity) {
        return new AddressDTO(
                entity.getId(),
                entity.getRecipient(),
                entity.getPhone(),
                entity.getLine1(),
                entity.getWard(),
                entity.getDistrict(),
                entity.getCity(),
                entity.isDefault()
        );
    }
}
