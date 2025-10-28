package com.alotra.service;

import com.alotra.entity.Branch;
import com.alotra.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BranchGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(BranchGeocodingService.class);

    private final BranchRepository branchRepository;
    private final GeocodingService geocodingService;

    @Transactional
    public int fixMissingCoordinates() {
        List<Branch> missing = branchRepository.findByLatitudeIsNullOrLongitudeIsNull();
        int updated = 0;
        for (Branch b : missing) {
            if (b.getAddress() == null || b.getAddress().isBlank()) continue;
            Optional<GeocodingService.LatLng> opt = geocodingService.geocodeAddress(b.getAddress());
            if (opt.isEmpty()) {
                // Try with country suffix
                opt = geocodingService.geocodeAddress(b.getAddress() + ", Vietnam");
            }
            if (opt.isPresent()) {
                GeocodingService.LatLng ll = opt.get();
                b.setLatitude(ll.latitude());
                b.setLongitude(ll.longitude());
                updated++;
            } else {
                log.warn("Cannot geocode branch {} with address '{}'", b.getId(), b.getAddress());
            }
        }
        if (updated > 0) branchRepository.saveAll(missing);
        return updated;
    }

    @Transactional
    public boolean geocodeBranch(Long branchId) {
        Branch b = branchRepository.findById(branchId).orElse(null);
        if (b == null || b.getAddress() == null || b.getAddress().isBlank()) return false;
        Optional<GeocodingService.LatLng> opt = geocodingService.geocodeAddress(b.getAddress());
        if (opt.isEmpty()) opt = geocodingService.geocodeAddress(b.getAddress() + ", Vietnam");
        if (opt.isEmpty()) return false;
        GeocodingService.LatLng ll = opt.get();
        b.setLatitude(ll.latitude());
        b.setLongitude(ll.longitude());
        branchRepository.save(b);
        return true;
    }
}