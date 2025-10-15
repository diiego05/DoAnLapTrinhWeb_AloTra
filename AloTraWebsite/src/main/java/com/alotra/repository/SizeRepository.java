package com.alotra.repository;

import com.alotra.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // Import thêm Optional

@Repository
public interface SizeRepository extends JpaRepository<Size, Long> {

    // Thêm phương thức này:
    // Tìm một Size theo tên, trả về Optional để xử lý trường hợp không tìm thấy
    Optional<Size> findByName(String name);
}