package com.alotra.controller.api;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/config")
public class PublicConfigController {

    @Value("${google.maps.apiKey:}")
    private String googleMapsApiKey;

    @GetMapping("/maps-key")
    public ResponseEntity<String> getGoogleMapsKey() {
        if (googleMapsApiKey == null || googleMapsApiKey.isBlank()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(googleMapsApiKey);
    }
}