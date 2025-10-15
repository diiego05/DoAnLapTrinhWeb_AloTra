package com.alotra.service;

import com.alotra.entity.*;
import com.alotra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final NotificationService notificationService;

    // === LẤY USER THEO EMAIL ===
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    // === CẬP NHẬT HỒ SƠ NGƯỜI DÙNG ===
    public User updateProfile(User user, User updated) {
        boolean changed = false;

        if (updated.getFullName() != null && !updated.getFullName().isBlank()) {
            user.setFullName(updated.getFullName());
            changed = true;
        }
        if (updated.getPhone() != null && !updated.getPhone().isBlank()) {
            user.setPhone(updated.getPhone());
            changed = true;
        }
        if (updated.getGender() != null && !updated.getGender().isBlank()) {
            user.setGender(updated.getGender());
            changed = true;
        }
        if (updated.getDateOfBirth() != null) {
            user.setDateOfBirth(updated.getDateOfBirth());
            changed = true;
        }
        if (updated.getAvatarUrl() != null && !updated.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(updated.getAvatarUrl());
            changed = true;
        }
        if (updated.getIdCardNumber() != null && !updated.getIdCardNumber().isBlank()) {
            // Kiểm tra CCCD trùng lặp
            User existed = userRepository.findByIdCardNumber(updated.getIdCardNumber()).orElse(null);
            if (existed != null && !existed.getId().equals(user.getId())) {
                throw new RuntimeException("Căn cước công dân đã được sử dụng cho tài khoản khác");
            }
            user.setIdCardNumber(updated.getIdCardNumber());
            changed = true;
        }

        if (changed) {
            userRepository.save(user);

            notificationService.create(
                    user.getId(),
                    "ACCOUNT_UPDATE",
                    "Cập nhật thông tin cá nhân",
                    "Bạn đã cập nhật hồ sơ cá nhân vào " + LocalDateTime.now(),
                    "USER",
                    user.getId()
            );
        }

        return user;
    }



    // === LẤY DANH SÁCH ĐỊA CHỈ ===
    public List<Address> getAddresses(Long userId) {
        return addressRepository.findByUserId(userId);
    }

    // === THÊM HOẶC SỬA ĐỊA CHỈ ===
    public Address addOrUpdateAddress(User user, Address address) {
        if (address.isDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .ifPresent(old -> {
                        old.setDefault(false);
                        addressRepository.save(old);
                    });
        }

        address.setUser(user);
        Address saved = addressRepository.save(address);

        notificationService.create(
                user.getId(),
                "ADDRESS_UPDATE",
                "Cập nhật địa chỉ",
                "Bạn đã " + (address.getId() == null ? "thêm" : "cập nhật") + " địa chỉ thành công.",
                "ADDRESS",
                saved.getId()
        );

        return saved;
    }

    // === XÓA ĐỊA CHỈ ===
    public void deleteAddress(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa địa chỉ này");
        }
        addressRepository.delete(address);

        notificationService.create(
                user.getId(),
                "ADDRESS_DELETE",
                "Xóa địa chỉ",
                "Bạn đã xóa một địa chỉ vào " + LocalDateTime.now(),
                "ADDRESS",
                addressId
        );
    }

    // === ĐẶT ĐỊA CHỈ MẶC ĐỊNH ===
    public Address setDefaultAddress(User user, Long addressId) {
        addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .ifPresent(old -> {
                    old.setDefault(false);
                    addressRepository.save(old);
                });

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa địa chỉ này");
        }

        address.setDefault(true);
        addressRepository.save(address);

        notificationService.create(
                user.getId(),
                "ADDRESS_DEFAULT",
                "Đặt địa chỉ mặc định",
                "Bạn đã đặt một địa chỉ mới làm mặc định.",
                "ADDRESS",
                addressId
        );

        return address;
    }


    public Address getDefaultAddress(User user) {
        return addressRepository.findByUserAndIsDefaultTrue(user).orElse(null);
    }
}
