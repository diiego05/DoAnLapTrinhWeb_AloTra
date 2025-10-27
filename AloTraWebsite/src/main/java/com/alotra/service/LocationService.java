package com.alotra.service;

import com.alotra.dto.BranchDistanceDTO;
import com.alotra.entity.Address;
import com.alotra.entity.Branch;
import com.alotra.repository.AddressRepository;
import com.alotra.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final BranchRepository branchRepository;
    private final AddressRepository addressRepository;
    private final GeocodingService geocodingService;
    private final UserService userService;

    private static final double EARTH_RADIUS_KM = 6371.0;

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    @Transactional(readOnly = true)
    public List<BranchDistanceDTO> findNearestBranches(double latitude, double longitude, Integer limit, String status) {
        int top = (limit == null || limit < 1) ? 5 : Math.min(limit, 50);

        // 🐛 DEBUG: Log toạ độ đầu vào
        System.out.println("🗺️ [LocationService] Finding nearest branches from coordinates:");
        System.out.println("   📍 User Location: lat=" + latitude + ", lng=" + longitude);

        List<Branch> branches = (status == null || status.isBlank())
                ? branchRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull()
                : branchRepository.findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(status);

        System.out.println("   🏪 Found " + branches.size() + " branches with coordinates");

        List<BranchDistanceDTO> result = branches.stream()
                .filter(b -> {
                    if (b.getLatitude() == null || b.getLongitude() == null) {
                        System.out.println("   ⚠️ Branch " + b.getName() + " has null coordinates");
                        return false;
                    }
                    if (!Double.isFinite(b.getLatitude()) || !Double.isFinite(b.getLongitude())) {
                        System.out.println("   ⚠️ Branch " + b.getName() + " has invalid coordinates (NaN/Infinity)");
                        return false;
                    }
                    return true;
                })
                .map(b -> {
                    double distance = haversineKm(latitude, longitude, b.getLatitude(), b.getLongitude());
                    // 🐛 DEBUG: Log từng chi nhánh với thông tin chi tiết
                    System.out.println("   🏢 Branch: " + b.getName());
                    System.out.println("      📍 Address: " + b.getAddress());
                    System.out.println("      📊 Coords: (" + b.getLatitude() + ", " + b.getLongitude() + ")");
                    System.out.println("      📏 Distance: " + String.format("%.2f km", distance));
                    
                    return BranchDistanceDTO.builder()
                            .id(b.getId())
                            .name(b.getName())
                            .address(b.getAddress())
                            .phone(b.getPhone())
                            .status(b.getStatus())
                            .latitude(b.getLatitude())
                            .longitude(b.getLongitude())
                            .distanceKm(distance)
                            .build();
                })
                .sorted(Comparator.comparing(BranchDistanceDTO::getDistanceKm))
                .limit(top)
                .toList();

        if (!result.isEmpty()) {
            System.out.println("   ✅ Nearest branch: " + result.get(0).getName() + 
                             " (" + String.format("%.2f km", result.get(0).getDistanceKm()) + ")");
            
            // Log top 3 để debug
            System.out.println("   📋 Top " + Math.min(3, result.size()) + " nearest branches:");
            result.stream().limit(3).forEach(item -> {
                System.out.println("      - " + item.getName() + ": " + 
                                 String.format("%.2f km", item.getDistanceKm()));
            });
        } else {
            System.out.println("   ⚠️ No branches found!");
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<BranchDistanceDTO> findNearestByAddress(String address, Integer limit, String status) {
        if (address == null || address.isBlank()) return List.of();
        Optional<GeocodingService.LatLng> ll = geocodingService.geocodeAddress(address);
        return ll.map(latLng -> findNearestBranches(latLng.latitude(), latLng.longitude(), limit, status))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<BranchDistanceDTO> findNearestForCurrentUser(Integer limit, String status) {
        Long userId = userService.getCurrentUserId();
        if (userId == null) return List.of();

        Optional<Address> opt = addressRepository.findByUser_IdOrderByIsDefaultDesc(userId)
                .stream().findFirst();
        if (opt.isEmpty()) return List.of();

        Address addr = opt.get();
        Double lat = addr.getLatitude();
        Double lon = addr.getLongitude();

        if (lat != null && lon != null) {
            return findNearestBranches(lat, lon, limit, status);
        }
        // Fallback: geocode on the fly
        String full = addr.getFullAddress();
        Optional<GeocodingService.LatLng> ll = geocodingService.geocodeAddress(full);
        return ll.map(latLng -> findNearestBranches(latLng.latitude(), latLng.longitude(), limit, status))
                .orElse(List.of());
    }
}