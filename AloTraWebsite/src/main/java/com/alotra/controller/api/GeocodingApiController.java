package com.alotra.controller.api;


import com.alotra.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
public class GeocodingApiController {

    private final GeocodingService geocodingService;

    @GetMapping("/geocode")
    public ResponseEntity<?> geocode(@RequestParam String address) {
        return geocodingService.geocodeAddress(address)
                .<ResponseEntity<?>>map(latLng -> ResponseEntity.ok(Map.of(
                        "latitude", latLng.latitude(),
                        "longitude", latLng.longitude()
                )))
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of(
                        "error", "Không tìm thấy toạ độ cho địa chỉ"
                )));
    }

    // 🔧 Debug endpoint: return raw Google Geocoding response (only for debugging)
    @GetMapping(value = "/google-raw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> googleRaw(@RequestParam String address) {
        return geocodingService.geocodeViaGoogleRaw(address)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}