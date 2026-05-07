package com.alotra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long NOMINATIM_DELAY_MS = 1200; // 1.2 giây để đảm bảo không vi phạm rate limit

    @Value("${google.maps.apiKey:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 🗺️ Geocode địa chỉ thành tọa độ với caching
     * Tự động fallback từ Google Maps sang Nominatim nếu cần
     */
    @Cacheable(value = "geocodeCache", key = "#address", unless = "#result == null")
    public Optional<LatLng> geocodeAddress(String address) {
        try {
            if (address == null || address.isBlank()) {
                logger.debug("Empty address provided, returning empty");
                return Optional.empty();
            }

            // 1️⃣ Thử Google Geocoding nếu có API key
            if (apiKey != null && !apiKey.isBlank() && !"YOUR_API_KEY_HERE".equals(apiKey)) {
                Optional<LatLng> googleResult = geocodeViaGoogleWithRetry(address);
                if (googleResult.isPresent()) {
                    logger.info("✅ Google geocode success for: {}", address);
                    return googleResult;
                }
                logger.debug("⚠️ Google geocoding failed, falling back to Nominatim");
            } else {
                logger.debug("ℹ️ Google API key not configured, using Nominatim");
            }

            // 2️⃣ Fallback: OpenStreetMap Nominatim (miễn phí)
            return geocodeViaNominatimWithRetry(address);

        } catch (Exception e) {
            logger.error("❌ Unexpected error geocoding address='{}': {}", address, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 🔄 Google Geocoding với retry logic
     */
    private Optional<LatLng> geocodeViaGoogleWithRetry(String address) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Optional<LatLng> result = geocodeViaGoogle(address);
                if (result.isPresent()) {
                    return result;
                }

                if (attempt < MAX_RETRIES) {
                    logger.debug("Retry {}/{} for Google geocoding: {}", attempt, MAX_RETRIES, address);
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * attempt);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted during retry");
                break;
            } catch (Exception e) {
                logger.warn("Attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());
            }
        }
        return Optional.empty();
    }

    /**
     * 🌍 Google Geocoding API call
     */
    private Optional<LatLng> geocodeViaGoogle(String address) {
        try {
            String url = String.format(
                "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s&language=vi&region=VN&bounds=8.1790665,102.14441|23.393395,109.4693&components=country:VN",
                URLEncoder.encode(address, StandardCharsets.UTF_8),
                apiKey
            );

            logger.debug("📡 Calling Google Geocoding API");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                logger.warn("⚠️ Google API returned non-2xx status: {}", resp.getStatusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            String status = root.path("status").asText("");

            // Xử lý các trạng thái của Google API
            switch (status) {
                case "OK":
                    return extractLatLngFromGoogle(root, address);
                case "ZERO_RESULTS":
                    logger.info("ℹ️ No results found for address: {}", address);
                    return Optional.empty();
                case "OVER_QUERY_LIMIT":
                    logger.warn("⚠️ Google API quota exceeded");
                    return Optional.empty();
                case "REQUEST_DENIED":
                    String errorMsg = root.path("error_message").asText("No error message");
                    logger.error("❌ Google API request denied: {}", errorMsg);
                    return Optional.empty();
                case "INVALID_REQUEST":
                    logger.warn("⚠️ Invalid request for address: {}", address);
                    return Optional.empty();
                default:
                    logger.warn("⚠️ Unexpected status from Google API: {}", status);
                    return Optional.empty();
            }

        } catch (RestClientException e) {
            logger.error("❌ Network error calling Google API: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("❌ Error parsing Google API response: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 📍 Trích xuất tọa độ từ Google API response
     */
    private Optional<LatLng> extractLatLngFromGoogle(JsonNode root, String address) {
        try {
            JsonNode results = root.get("results");
            if (results == null || !results.isArray() || results.size() == 0) {
                logger.info("ℹ️ Empty results array from Google");
                return Optional.empty();
            }

            JsonNode firstResult = results.get(0);
            JsonNode location = firstResult.path("geometry").path("location");

            if (!location.has("lat") || !location.has("lng")) {
                logger.warn("⚠️ Missing lat/lng in Google response");
                return Optional.empty();
            }

            double lat = location.get("lat").asDouble();
            double lng = location.get("lng").asDouble();

            // Validate coordinates are in Vietnam bounds
            if (!isValidVietnameseCoordinates(lat, lng)) {
                logger.warn("⚠️ Coordinates outside Vietnam: lat={}, lng={}", lat, lng);
            }

            logger.debug("✅ Google geocode: {} -> lat={}, lng={}", address, lat, lng);
            return Optional.of(new LatLng(lat, lng));

        } catch (Exception e) {
            logger.error("❌ Error extracting coordinates: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 🔄 Nominatim với retry và multiple address formats
     */
    private Optional<LatLng> geocodeViaNominatimWithRetry(String address) {
        // Tạo nhiều biến thể của địa chỉ để tăng khả năng tìm thấy
        List<String> addressVariants = generateAddressVariants(address);

        for (String variant : addressVariants) {
            logger.debug("🔍 Trying Nominatim with variant: {}", variant);

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    Optional<LatLng> result = geocodeViaNominatim(variant);
                    if (result.isPresent()) {
                        logger.info("✅ Nominatim success with variant: {}", variant);
                        return result;
                    }

                    if (attempt < MAX_RETRIES) {
                        logger.debug("Retry {}/{} for Nominatim", attempt, MAX_RETRIES);
                        TimeUnit.MILLISECONDS.sleep(NOMINATIM_DELAY_MS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted during Nominatim retry");
                    return Optional.empty();
                } catch (Exception e) {
                    logger.warn("Nominatim attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());
                }
            }

            // Delay giữa các variant để tránh rate limit
            try {
                TimeUnit.MILLISECONDS.sleep(NOMINATIM_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.warn("❌ All Nominatim attempts failed for address: {}", address);
        return Optional.empty();
    }

    /**
     * 🔀 Tạo nhiều biến thể địa chỉ để tăng tỷ lệ thành công
     */
    private List<String> generateAddressVariants(String address) {
        List<String> variants = new ArrayList<>();

        // 1. Địa chỉ gốc
        variants.add(address);

        // 2. Thêm "Vietnam" nếu chưa có
        if (!address.toLowerCase().contains("việt nam") && !address.toLowerCase().contains("vietnam")) {
            variants.add(address + ", Vietnam");
            variants.add(address + ", Việt Nam");
        }

        // 3. Thử format ngắn gọn hơn (bỏ số nhà nếu có)
        String[] parts = address.split(",");
        if (parts.length > 2) {
            // Bỏ phần đầu (số nhà), giữ lại quận/thành phố
            String simplified = String.join(",", Arrays.copyOfRange(parts, 1, parts.length)).trim();
            if (!simplified.isEmpty()) {
                variants.add(simplified);
                if (!simplified.toLowerCase().contains("vietnam")) {
                    variants.add(simplified + ", Vietnam");
                }
            }
        }

        // 4. Thử chỉ với thành phố cuối cùng
        if (parts.length >= 2) {
            String cityOnly = parts[parts.length - 1].trim();
            if (!cityOnly.isEmpty() && !cityOnly.matches("\\d+")) {
                variants.add(cityOnly + ", Vietnam");
            }
        }

        // Loại bỏ duplicate
        return variants.stream().distinct().toList();
    }

    /**
     * 🗺️ Nominatim fallback (OpenStreetMap)
     */
    private Optional<LatLng> geocodeViaNominatim(String address) {
        try {
            // ✅ Delay để tránh rate limit
            Thread.sleep(NOMINATIM_DELAY_MS);

            String url = String.format(
                "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1&countrycodes=vn&addressdetails=1",
                URLEncoder.encode(address, StandardCharsets.UTF_8)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "AloTraWebsite/1.0 (support@alotra.vn)");
            headers.set("Accept", "application/json");
            headers.set("Accept-Language", "vi,en");

            logger.debug("📡 Calling Nominatim API for: {}", address);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                logger.warn("⚠️ Nominatim returned status: {}", resp.getStatusCode());
                return Optional.empty();
            }

            logger.debug("📥 Nominatim response: {}", resp.getBody());

            JsonNode arr = objectMapper.readTree(resp.getBody());
            if (!arr.isArray() || arr.size() == 0) {
                logger.debug("⚠️ Nominatim: No results for '{}'", address);
                return Optional.empty();
            }

            JsonNode first = arr.get(0);
            if (!first.has("lat") || !first.has("lon")) {
                logger.warn("⚠️ Nominatim result missing coordinates");
                return Optional.empty();
            }

            double lat = first.get("lat").asDouble();
            double lon = first.get("lon").asDouble();

            // Validate coordinates
            if (!isValidVietnameseCoordinates(lat, lon)) {
                logger.warn("⚠️ Coordinates outside Vietnam bounds: lat={}, lng={}", lat, lon);
                // Vẫn return vì có thể là khu vực biên giới
            }

            logger.info("✅ Nominatim SUCCESS: {} -> lat={}, lng={}", address, lat, lon);
            return Optional.of(new LatLng(lat, lon));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("❌ Thread interrupted during Nominatim call");
            return Optional.empty();
        } catch (RestClientException e) {
            logger.error("❌ Network error calling Nominatim: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("❌ Error with Nominatim: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * ✅ Kiểm tra tọa độ có nằm trong Việt Nam không
     */
    private boolean isValidVietnameseCoordinates(double lat, double lng) {
        // Vietnam bounds: 8.1790665,102.14441 đến 23.393395,109.4693
        return lat >= 8.0 && lat <= 24.0 && lng >= 102.0 && lng <= 110.0;
    }

    /**
     * 🔍 Debug endpoint - lấy raw response từ Google
     */
    public Optional<String> geocodeViaGoogleRaw(String address) {
        try {
            if (address == null || address.isBlank()) return Optional.empty();
            if (apiKey == null || apiKey.isBlank()) return Optional.empty();

            String url = String.format(
                "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s&language=vi&region=VN",
                URLEncoder.encode(address, StandardCharsets.UTF_8),
                apiKey
            );

            logger.debug("[DEBUG] Google API URL: {}", url.replaceAll("key=[^&]+", "key=***"));
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                logger.warn("[DEBUG] HTTP error: {}", resp.getStatusCode());
                return Optional.empty();
            }

            return Optional.of(resp.getBody());

        } catch (Exception e) {
            logger.error("[DEBUG] Error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 📌 Value class cho tọa độ
     */
    public record LatLng(double latitude, double longitude) {
        public LatLng {
            if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
                throw new IllegalArgumentException("Invalid coordinates");
            }
        }
    }
}