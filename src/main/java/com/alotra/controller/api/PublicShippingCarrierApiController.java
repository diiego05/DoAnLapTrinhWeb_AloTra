// com/alotra/controller/api/PublicShippingCarrierApiController.java
package com.alotra.controller.api;

import com.alotra.dto.ShippingFeeDTO;
import com.alotra.entity.ShippingCarrier;
import com.alotra.service.ShippingCarrierService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public/shipping-carriers")
@RequiredArgsConstructor
public class PublicShippingCarrierApiController {
    private final ShippingCarrierService service;

    @GetMapping
    public ResponseEntity<?> getCarriers() {
        return ResponseEntity.ok(service.getActiveCarriers());
    }

    @GetMapping("/{id}")
    public ShippingCarrier getCarrierById(@PathVariable Long id) {
        return service.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà vận chuyển"));
    }

    @GetMapping("/{id}/fee")
    public ResponseEntity<ShippingFeeDTO> getCarrierFee(@PathVariable Long id) {
        return ResponseEntity.ok(service.getFeeDetail(id));
    }

}
