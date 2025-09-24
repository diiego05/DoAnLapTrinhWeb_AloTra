package com.alotra.service;

import com.alotra.entity.Address; // << Thêm import
import com.alotra.entity.Role;
import com.alotra.entity.User;
import com.alotra.repository.AddressRepository; // << Thêm import
import com.alotra.repository.RoleRepository;
import com.alotra.repository.UserRepository;
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
                     String addressLine1, String addressCity, String addressDistrict, String addressWard) {

        User userInDb;
        // Kiểm tra là Sửa hay Thêm mới
        if (userFromForm.getId() != null) {
            userInDb = userRepository.findById(userFromForm.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            userInDb = userFromForm;
        }

        // 1. Xử lý mật khẩu
        if (StringUtils.hasText(userFromForm.getRawPassword())) {
            userInDb.setPasswordHash(passwordEncoder.encode(userFromForm.getRawPassword()));
        }

        // 2. Xử lý ảnh đại diện
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = cloudinaryService.uploadFile(avatarFile);
            userInDb.setAvatarUrl(avatarUrl);
        }

        // 3. Cập nhật các trường thông tin khác
        userInDb.setFullName(userFromForm.getFullName());
        userInDb.setEmail(userFromForm.getEmail());
        userInDb.setPhone(userFromForm.getPhone());
        userInDb.setGender(userFromForm.getGender());
        userInDb.setDateOfBirth(userFromForm.getDateOfBirth());
        userInDb.setIdCardNumber(userFromForm.getIdCardNumber());
        userInDb.setRole(userFromForm.getRole());
        userInDb.setStatus(userFromForm.getStatus());

        // 4. Lưu User để có ID (quan trọng cho việc tạo địa chỉ mới)
        User savedUser = userRepository.save(userInDb);

        // 5. XỬ LÝ LƯU ĐỊA CHỈ MỚI
        // Chỉ tạo địa chỉ nếu vai trò là USER và có nhập dòng địa chỉ đầu tiên
        if (savedUser.getRole().getCode().equals("USER") && StringUtils.hasText(addressLine1)) {
            Address newAddress = new Address();
            newAddress.setUser(savedUser);
            newAddress.setRecipient(savedUser.getFullName());
            newAddress.setPhone(savedUser.getPhone());
            newAddress.setLine1(addressLine1);
            newAddress.setCity(addressCity);
            newAddress.setDistrict(addressDistrict);
            newAddress.setWard(addressWard);
            newAddress.setDefault(true); // Tạm thời gán địa chỉ đầu tiên là mặc định

            addressRepository.save(newAddress);
        }
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}