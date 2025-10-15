package com.alotra.controller.api;

import com.alotra.dto.AddressDTO;
import com.alotra.dto.UserProfileDTO;
import com.alotra.entity.Address;
import com.alotra.entity.User;
import com.alotra.service.CloudinaryService;
import com.alotra.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileApiController {

    private final ProfileService profileService;
    private final CloudinaryService cloudinaryService;

    // ==================== LẤY THÔNG TIN CÁ NHÂN ====================
    @GetMapping
    public ResponseEntity<UserProfileDTO> getProfile(Principal principal) {
        User user = profileService.getUserByEmail(principal.getName());
        UserProfileDTO dto = new UserProfileDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getAvatarUrl(),
                user.getIdCardNumber()
        );
        return ResponseEntity.ok(dto);
    }

    // ==================== CẬP NHẬT THÔNG TIN CÁ NHÂN ====================
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileDTO> updateProfile(
            @RequestPart("data") UserProfileDTO form,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Principal principal
    ) {
        User current = profileService.getUserByEmail(principal.getName());

        // Upload avatar nếu có file mới
        if (file != null && !file.isEmpty()) {
            String imageUrl = cloudinaryService.uploadFile(file);
            current.setAvatarUrl(imageUrl);
        }

        // Cập nhật các thông tin khác
        if (form.getFullName() != null && !form.getFullName().isBlank())
            current.setFullName(form.getFullName());
        if (form.getPhone() != null && !form.getPhone().isBlank())
            current.setPhone(form.getPhone());
        if (form.getGender() != null)
            current.setGender(form.getGender());
        if (form.getDateOfBirth() != null)
            current.setDateOfBirth(form.getDateOfBirth());
        if (form.getIdCardNumber() != null && !form.getIdCardNumber().isBlank())
            current.setIdCardNumber(form.getIdCardNumber());

        User updated = profileService.updateProfile(current, current);

        UserProfileDTO dto = new UserProfileDTO(
                updated.getId(),
                updated.getFullName(),
                updated.getEmail(),
                updated.getPhone(),
                updated.getGender(),
                updated.getDateOfBirth(),
                updated.getAvatarUrl(),
                updated.getIdCardNumber()
        );
        return ResponseEntity.ok(dto);
    }

    // ==================== CẬP NHẬT ẢNH ĐẠI DIỆN RIÊNG ====================
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileDTO> updateAvatar(
            @RequestPart("file") MultipartFile file,
            Principal principal
    ) {
        User current = profileService.getUserByEmail(principal.getName());
        String imageUrl = cloudinaryService.uploadFile(file);
        current.setAvatarUrl(imageUrl);
        User updated = profileService.updateProfile(current, current);

        UserProfileDTO dto = new UserProfileDTO(
                updated.getId(),
                updated.getFullName(),
                updated.getEmail(),
                updated.getPhone(),
                updated.getGender(),
                updated.getDateOfBirth(),
                updated.getAvatarUrl(),
                updated.getIdCardNumber()
        );
        return ResponseEntity.ok(dto);
    }

    // ==================== QUẢN LÝ ĐỊA CHỈ NGƯỜI DÙNG (SỬ DỤNG DTO) ====================
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getAddresses(Principal principal) {
        User user = profileService.getUserByEmail(principal.getName());
        List<AddressDTO> dtoList = profileService.getAddresses(user.getId())
                .stream()
                .map(AddressDTO::from)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> addOrUpdateAddress(
            @RequestBody Address address,
            Principal principal
    ) {
        User user = profileService.getUserByEmail(principal.getName());
        Address saved = profileService.addOrUpdateAddress(user, address);
        return ResponseEntity.ok(AddressDTO.from(saved));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            Principal principal
    ) {
        User user = profileService.getUserByEmail(principal.getName());
        profileService.deleteAddress(user, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/addresses/{id}/default")
    public ResponseEntity<AddressDTO> setDefaultAddress(
            @PathVariable Long id,
            Principal principal
    ) {
        User user = profileService.getUserByEmail(principal.getName());
        Address updated = profileService.setDefaultAddress(user, id);
        return ResponseEntity.ok(AddressDTO.from(updated));
    }

    @GetMapping("/api/profile/addresses/default")
    public ResponseEntity<?> getDefaultAddress(Principal principal) {
        User user = profileService.getUserByEmail(principal.getName());
        Address addr = profileService.getDefaultAddress(user);
        return ResponseEntity.ok(addr != null ? AddressDTO.from(addr) : null);
    }

}
