package com.alotra.repository;

import com.alotra.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Tự động tìm kiếm một Role dựa vào trường 'code'.
     * @param code Mã của vai trò (VD: "USER", "ADMIN").
     * @return một Optional chứa Role nếu tìm thấy.
     */
    Optional<Role> findByCode(String code);
    Optional<Role> findByName(String name);
}