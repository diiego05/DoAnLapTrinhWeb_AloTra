package com.alotra.controller.api;

import com.alotra.entity.ShippingCarrier;
import com.alotra.service.ShippingCarrierService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping-carriers")
public class ShippingCarrierApiController {

    private final ShippingCarrierService service;

    public ShippingCarrierApiController(ShippingCarrierService service) {
        this.service = service;
    }

    // 🌐 Lấy danh sách nhà vận chuyển đang hoạt động
    @GetMapping("/active")
    public List<ShippingCarrier> getActiveCarriers() {
        return service.getActiveCarriers();
    }
}
