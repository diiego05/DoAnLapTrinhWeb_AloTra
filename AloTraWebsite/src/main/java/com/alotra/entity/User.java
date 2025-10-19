package com.alotra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.alotra.validator.StrongPassword;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Data
@Entity
@Table(name = "Users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Column(name = "Email", unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Column(name = "Phone", unique = true)
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @StrongPassword
    @Column(name = "PasswordHash", nullable = false)
    private String passwordHash;

    @NotBlank(message = "Họ tên không được để trống")
    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "AvatarUrl")
    private String avatarUrl;

    @Column(name = "Gender")
    private String gender;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "IdCardNumber", unique = true)
    private String idCardNumber;

    @Column(name = "EmailVerifiedAt")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "Status", nullable = false)
    private String status; // ACTIVE | BANNED | INACTIVE

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RoleId", nullable = false)
    private Role role;

    @Transient
    private String rawPassword;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Address> addresses;

    @Column(name = "FailedLoginAttempts")
    private int failedLoginAttempts = 0;

    @Column(name = "LockoutEnd")
    private LocalDateTime lockoutEnd;


    // --- Lifecycle ---
    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public User(Long id) {
        this.id = id;
    }

	public User() {
	}
    // --- Lấy địa chỉ mặc định ---
    public Address getDefaultAddress() {
        if (addresses == null || addresses.isEmpty()) return null;
        return addresses.stream()
                .filter(Address::isDefault)
                .findFirst()
                .orElse(addresses.get(0));
    }

    // === SPRING SECURITY IMPLEMENTATION ===
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Đảm bảo không bị trùng tiền tố ROLE_
        String code = this.role != null ? this.role.getCode().toUpperCase() : "USER";
        if (!code.startsWith("ROLE_")) {
            code = "ROLE_" + code;
        }
        return Collections.singletonList(new SimpleGrantedAuthority(code));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Bị khóa nếu status = "BANNED" hoặc còn trong thời gian lockout
        if ("BANNED".equalsIgnoreCase(this.status)) return false;
        if (lockoutEnd != null && lockoutEnd.isAfter(LocalDateTime.now())) return false;
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private ChatRoom chatRoom;
}
