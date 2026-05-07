package com.alotra.service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.alotra.entity.Address; // << Thêm import
import com.alotra.entity.Role;
import com.alotra.entity.Shipper;
import com.alotra.entity.User;
import com.alotra.repository.AddressRepository; // << Thêm import
import com.alotra.repository.RoleRepository;
import com.alotra.repository.ShipperRepository;
import com.alotra.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // << Thêm import
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AddressRepository addressRepository; // << Tiêm AddressRepository
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private ShipperRepository shipperRepository;

    public List<User> searchAndFilter(String keyword, Long roleId, String status) {
        return userRepository.searchAndFilter(keyword, roleId,
                StringUtils.hasText(status) ? status : null);
    }

    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional // Đảm bảo tất cả các thao tác DB thành công hoặc thất bại cùng nhau
    public void save(User userFromForm, MultipartFile avatarFile,
                     String addressLine1, String addressCity, String addressWard) {
    	Logger log = LoggerFactory.getLogger(getClass());
        log.info("🟢 [SERVICE] Bắt đầu lưu người dùng...");
        log.info("👉 ID: {}", userFromForm.getId());
        log.info("👉 FullName: {}", userFromForm.getFullName());
        log.info("👉 Email: {}", userFromForm.getEmail());
        log.info("👉 Phone: {}", userFromForm.getPhone());
        log.info("👉 Role ID: {}", userFromForm.getRole() != null ? userFromForm.getRole().getId() : null);
        log.info("👉 Raw Password: {}", userFromForm.getRawPassword());
        log.info("👉 Address nhập: {} - {} - {}", addressLine1, addressWard, addressCity);
        User userInDb;
        // Kiểm tra là Sửa hay Thêm mới
        if (userFromForm.getId() != null) {
        	userInDb = userRepository.findById(userFromForm.getId())
                    .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy người dùng để cập nhật"));
            log.info("✏️ [SERVICE] Cập nhật người dùng hiện có ID = {}", userInDb.getId());
        } else {
        	userInDb = new User();
            log.info("➕ [SERVICE] Tạo mới người dùng");
        }

        // 1. Xử lý mật khẩu
        if (StringUtils.hasText(userFromForm.getRawPassword())) {
        	 String encoded = passwordEncoder.encode(userFromForm.getRawPassword());
             userInDb.setPasswordHash(encoded);
             log.info("✅ Mật khẩu đã encode");
         } else {
             log.info("⚠️ Không nhập mật khẩu (giữ nguyên nếu sửa)");
        }

        // 2. Xử lý ảnh đại diện
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = cloudinaryService.uploadFile(avatarFile);
            userInDb.setAvatarUrl(avatarUrl);
            log.info("✅ Ảnh đại diện đã upload: {}", avatarUrl);
        }

        // 3. Cập nhật các trường thông tin khác
        userInDb.setFullName(userFromForm.getFullName());
        userInDb.setEmail(userFromForm.getEmail());
        userInDb.setPhone(userFromForm.getPhone());
        userInDb.setGender(userFromForm.getGender());
        userInDb.setDateOfBirth(userFromForm.getDateOfBirth());
        userInDb.setIdCardNumber(userFromForm.getIdCardNumber());

        userInDb.setStatus(userFromForm.getStatus());
        if (userFromForm.getRole() != null && userFromForm.getRole().getId() != null) {
            Role role = roleRepository.findById(userFromForm.getRole().getId())
                    .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy Role ID: " + userFromForm.getRole().getId()));
            userInDb.setRole(role);
            log.info("✅ Role đã gán: {} - {}", role.getId(), role.getCode());
        } else {
            log.warn("⚠️ Role không hợp lệ (null hoặc không có ID)");
        }
        // 4. Lưu User để có ID (quan trọng cho việc tạo địa chỉ mới)
        User savedUser = userRepository.save(userInDb);
        log.info("💾 [SERVICE] User đã lưu thành công — ID = {}", savedUser.getId());

        // 5. Xử lý địa chỉ nếu là USER
        if (savedUser.getRole() != null
                && "USER".equalsIgnoreCase(savedUser.getRole().getCode())
                && StringUtils.hasText(addressLine1)) {
            log.info("🏠 [SERVICE] Thêm địa chỉ mặc định cho USER ID = {}", savedUser.getId());

            Address newAddress = new Address();
            newAddress.setUser(savedUser);
            newAddress.setRecipient(savedUser.getFullName());
            newAddress.setPhone(savedUser.getPhone());
            newAddress.setLine1(addressLine1);
            newAddress.setCity(addressCity);
            newAddress.setWard(addressWard);
            newAddress.setDefault(true); // Tạm thời gán địa chỉ đầu tiên là mặc định

            addressRepository.save(newAddress);
            log.info("✅ Đã lưu địa chỉ mặc định cho user ID = {}", savedUser.getId());
        } else {
            log.info("⚠️ Không thêm địa chỉ (không phải USER hoặc không nhập địa chỉ)");
        }
        log.info("🎯 [SERVICE] Hoàn tất xử lý lưu người dùng.");
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public User getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }
        return user;
    }
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        // Nếu username là email → lấy từ DB ra ID
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));
        return user.getId();
    }
    public Long getCurrentShipperId() {
        Long userId = getCurrentUserId();
        Shipper shipper = shipperRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản này không phải shipper"));
        return shipper.getId();
    }

}