package com.alotra.controller.api;

import com.alotra.entity.ShippingCarrier;
import com.alotra.service.ShippingCarrierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shipping-carriers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminShippingCarrierApiController {

    private final ShippingCarrierService service;

    public AdminShippingCarrierApiController(ShippingCarrierService service) {
        this.service = service;
    }

    // 📋 Lấy toàn bộ nhà vận chuyển
    @GetMapping
    public List<ShippingCarrier> getAll() {
        return service.getAll();
    }

    // ➕ Thêm mới
    @PostMapping
    public ResponseEntity<ShippingCarrier> create(@RequestBody ShippingCarrier carrier) {
        return ResponseEntity.ok(service.create(carrier));
    }

    // ✏️ Cập nhật
    @PutMapping("/{id}")
    public ResponseEntity<ShippingCarrier> update(@PathVariable Long id, @RequestBody ShippingCarrier carrier) {
        return ResponseEntity.ok(service.update(id, carrier));
    }

    // 🔁 Bật / Tắt hoạt động
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Void> toggle(@PathVariable Long id) {
        service.toggleStatus(id);
        return ResponseEntity.ok().build();
    }

    // 🗑️ Xóa
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
