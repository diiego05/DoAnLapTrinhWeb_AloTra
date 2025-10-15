package com.alotra.controller.api;

import com.alotra.dto.AddressDTO;
import com.alotra.service.AddressService;
import com.alotra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressApiController {

    private final AddressService addressService;
    private final UserService userService;

    /**
     * 🏡 Lấy danh sách địa chỉ của user hiện tại
     */
    @GetMapping
    public ResponseEntity<List<AddressDTO>> myAddresses() {
        Long uid = userService.getCurrentUserId();
        return ResponseEntity.ok(addressService.getUserAddresses(uid));
    }

    /**
     * 🆕 Tạo mới một địa chỉ giao hàng
     */
    @PostMapping
    public ResponseEntity<AddressDTO> create(@RequestBody AddressDTO dto) {
        Long uid = userService.getCurrentUserId();
        return ResponseEntity.ok(addressService.createAddress(uid, dto));
    }

    /**
     * 🌟 Đặt một địa chỉ làm mặc định
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<Void> setDefault(@PathVariable Long id) {
        Long uid = userService.getCurrentUserId();
        addressService.setDefaultAddress(uid, id);
        return ResponseEntity.ok().build();
    }

    /**
     * 🗑️ Xóa một địa chỉ
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long uid = userService.getCurrentUserId();
        addressService.deleteAddress(uid, id);
        return ResponseEntity.ok().build();
    }
}
