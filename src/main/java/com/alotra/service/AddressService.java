package com.alotra.service;

import com.alotra.dto.AddressDTO;
import com.alotra.entity.Address;
import com.alotra.entity.User;
import com.alotra.repository.AddressRepository;
import com.alotra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;
    /**
     * 📦 Lấy danh sách địa chỉ của user, địa chỉ mặc định sẽ lên đầu.
     */
    @Transactional(readOnly = true)
    public List<AddressDTO> getUserAddresses(Long userId) {
        return addressRepository.findByUser_IdOrderByIsDefaultDesc(userId)
                .stream()
                .map(AddressDTO::from)
                .toList();
    }

    /**
     * 🆕 Thêm mới địa chỉ cho user.
     * Nếu địa chỉ được set là mặc định -> hủy mặc định cũ.
     */
    @Transactional
    public AddressDTO createAddress(Long userId, AddressDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        if (dto.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = new Address();
        address.setUser(user);
        address.setRecipient(dto.recipient());
        address.setPhone(dto.phone());
        address.setLine1(dto.line1());
        address.setWard(dto.ward());
        address.setCity(dto.city());
        address.setDefault(dto.isDefault());

        boolean setFromClient = setIfValidCoordinates(address, dto.latitude(), dto.longitude());

        // 🌐 Nếu client không gửi hoặc không hợp lệ -> geocode server-side
        if (!setFromClient) {
            String fullAddress = address.getFullAddressForGeocoding();
            System.out.println("🗺️ [AddressService] Geocoding address: " + fullAddress);

            geocodingService.geocodeAddress(fullAddress)
                    .ifPresent(ll -> {
                        System.out.println("✅ [AddressService] Geocoded successfully - lat: " + ll.latitude() + ", lng: " + ll.longitude());
                        address.setLatitude(ll.latitude());
                        address.setLongitude(ll.longitude());
                    });

            if (address.getLatitude() == null || address.getLongitude() == null) {
                System.out.println("⚠️ [AddressService] Geocoding failed for: " + fullAddress);
            }
        } else {
            System.out.println("✅ [AddressService] Using client-provided coordinates: lat=" + address.getLatitude() + ", lng=" + address.getLongitude());
        }

        Address saved = addressRepository.save(address);
        System.out.println("💾 [AddressService] Saved address ID=" + saved.getId() + " with coords: (" + saved.getLatitude() + ", " + saved.getLongitude() + ")");

        return AddressDTO.from(saved);
    }
    @Transactional
    public AddressDTO updateAddress(Long userId, Long addressId, AddressDTO dto) {
        Address address = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ"));

        // Cập nhật thông tin
        address.setRecipient(dto.recipient());
        address.setPhone(dto.phone());
        address.setLine1(dto.line1());
        address.setWard(dto.ward());
        address.setCity(dto.city());

        // Nếu set là mặc định, xóa mặc định cũ
        if (dto.isDefault() && !address.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
            address.setDefault(true);
        } else if (!dto.isDefault() && address.isDefault()) {
            address.setDefault(false);
        }
        boolean setFromClient = setIfValidCoordinates(address, dto.latitude(), dto.longitude());

        // 🌐 Nếu client không gửi hoặc không hợp lệ -> geocode lại với địa chỉ đầy đủ kèm "Vietnam"
        if (!setFromClient) {
            String fullAddress = address.getFullAddressForGeocoding();
            System.out.println("🗺️ [DEBUG] Re-geocoding address: " + fullAddress);

            geocodingService.geocodeAddress(fullAddress)
                    .ifPresent(ll -> {
                        System.out.println("✅ [DEBUG] Re-geocoded successfully - lat: " + ll.latitude() + ", lng: " + ll.longitude());
                        address.setLatitude(ll.latitude());
                        address.setLongitude(ll.longitude());
                    });
        }
        return AddressDTO.from(addressRepository.save(address));
    }
    /**
     * 🌟 Đặt địa chỉ mặc định cho user.
     * Nếu địa chỉ không thuộc user -> không thực hiện.
     */
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        addressRepository.clearDefaultForUser(userId);
        addressRepository.findByIdAndUser_Id(addressId, userId)
                .ifPresent(addr -> {
                    addr.setDefault(true);
                    addressRepository.save(addr);
                });
    }

    /**
     * 🗑️ Xóa địa chỉ của user.
     */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        addressRepository.findByIdAndUser_Id(addressId, userId)
                .ifPresent(addressRepository::delete);
    }

    /**
     * 📌 Snapshot địa chỉ: dùng khi lưu vào Order
     * Trả về chuỗi đầy đủ: "line1, ward, district, city (Recipient - Phone)"
     */
    @Transactional(readOnly = true)
    public String snapshotAddress(Long addressId, Long userId, String paymentMethod) {
        Address address = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ hợp lệ"));

        return String.format(
        		"%s, %s, %s (%s - %s)",
                address.getLine1(),
                address.getWard(),

                address.getCity(),
                address.getRecipient(),
                address.getPhone()
        );
    }

    /**
     * 📥 Lấy địa chỉ mặc định (nếu có).
     */
    @Transactional(readOnly = true)
    public AddressDTO getDefaultAddress(Long userId) {
        return addressRepository.findByUser_IdOrderByIsDefaultDesc(userId)
                .stream()
                .findFirst()
                .map(AddressDTO::from)
                .orElse(null);
    }
    @Transactional(readOnly = true)
    public Optional<GeocodingService.LatLng> getCoordinates(Long userId, Long addressId) {
        // ✅ Nếu có userId, kiểm tra quyền sở hữu
        if (userId != null) {
            return addressRepository.findByIdAndUser_Id(addressId, userId)
                    .map(a -> a.getLatitude() != null && a.getLongitude() != null
                            ? new GeocodingService.LatLng(a.getLatitude(), a.getLongitude())
                            : null)
                    .map(Optional::ofNullable)
                    .orElse(Optional.empty());
        }

        // ✅ Nếu userId = null, cho phép query address bất kỳ (dùng cho API public)
        return addressRepository.findById(addressId)
                .map(a -> a.getLatitude() != null && a.getLongitude() != null
                        ? new GeocodingService.LatLng(a.getLatitude(), a.getLongitude())
                        : null)
                .map(Optional::ofNullable)
                .orElse(Optional.empty());
    }

    // =========================
    // 🔎 Helpers
    // =========================
    private boolean setIfValidCoordinates(Address address, Double lat, Double lng) {
        if (lat == null || lng == null) return false;
        if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
            System.out.println("⚠️ [AddressService] Invalid coordinates (NaN or Infinity): lat=" + lat + ", lng=" + lng);
            return false;
        }
        if (!isValidVietnameseCoordinates(lat, lng)) {
            System.out.println("⚠️ [AddressService] Coordinates outside Vietnam bounds: lat=" + lat + ", lng=" + lng);
            return false;
        }
        address.setLatitude(lat);
        address.setLongitude(lng);
        return true;
    }

    private boolean isValidVietnameseCoordinates(double lat, double lng) {
        return lat >= 8.0 && lat <= 24.5 && lng >= 102.0 && lng <= 110.5;
    }

}
