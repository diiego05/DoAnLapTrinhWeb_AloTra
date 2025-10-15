package com.alotra.service;

import com.alotra.dto.AddressDTO;
import com.alotra.entity.Address;
import com.alotra.entity.User;
import com.alotra.repository.AddressRepository;
import com.alotra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * 📦 Lấy danh sách địa chỉ của user, địa chỉ mặc định sẽ lên đầu.
     */
    @Transactional(readOnly = true)
    public List<AddressDTO> getUserAddresses(Long userId) {
        return addressRepository.findByUser_IdOrderByIsDefaultDesc(userId)
                .stream()
                .map(AddressDTO::from)
                .toList();
    }

    /**
     * 🆕 Thêm mới địa chỉ cho user.
     * Nếu địa chỉ được set là mặc định -> hủy mặc định cũ.
     */
    @Transactional
    public AddressDTO createAddress(Long userId, AddressDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        if (dto.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = new Address();
        address.setUser(user);
        address.setRecipient(dto.recipient());
        address.setPhone(dto.phone());
        address.setLine1(dto.line1());
        address.setWard(dto.ward());
        address.setDistrict(dto.district());
        address.setCity(dto.city());
        address.setDefault(dto.isDefault());

        return AddressDTO.from(addressRepository.save(address));
    }

    /**
     * 🌟 Đặt địa chỉ mặc định cho user.
     * Nếu địa chỉ không thuộc user -> không thực hiện.
     */
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        addressRepository.clearDefaultForUser(userId);
        addressRepository.findByIdAndUser_Id(addressId, userId)
                .ifPresent(addr -> {
                    addr.setDefault(true);
                    addressRepository.save(addr);
                });
    }

    /**
     * 🗑️ Xóa địa chỉ của user.
     */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        addressRepository.findByIdAndUser_Id(addressId, userId)
                .ifPresent(addressRepository::delete);
    }

    /**
     * 📌 Snapshot địa chỉ: dùng khi lưu vào Order
     * Trả về chuỗi đầy đủ: "line1, ward, district, city (Recipient - Phone)"
     */
    @Transactional(readOnly = true)
    public String snapshotAddress(Long addressId, Long userId, String paymentMethod) {
        Address address = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ hợp lệ"));

        return String.format(
                "%s, %s, %s, %s (%s - %s)",
                address.getLine1(),
                address.getWard(),
                address.getDistrict(),
                address.getCity(),
                address.getRecipient(),
                address.getPhone()
        );
    }

    /**
     * 📥 Lấy địa chỉ mặc định (nếu có).
     */
    @Transactional(readOnly = true)
    public AddressDTO getDefaultAddress(Long userId) {
        return addressRepository.findByUser_IdOrderByIsDefaultDesc(userId)
                .stream()
                .findFirst()
                .map(AddressDTO::from)
                .orElse(null);
    }
}
