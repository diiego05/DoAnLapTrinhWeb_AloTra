package com.alotra.service;

import com.alotra.entity.Size;
import com.alotra.repository.SizeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class SizeService {

    @Autowired
    private SizeRepository sizeRepository;

    // Phương thức tiện ích để tạo slug/code
    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toUpperCase().replace('Đ', 'D');
    }

    public List<Size> findAll() {
        return sizeRepository.findAll();
    }

    public Size findById(Long id) {
        return sizeRepository.findById(id).orElse(null);
    }

    // THAY THẾ PHƯƠNG THỨC SAVE CŨ BẰNG PHƯƠNG THỨC MỚI NÀY
    public void save(Size size) throws DataIntegrityViolationException {

        // ---- PHẦN THÊM MỚI QUAN TRỌNG ----
        // 1. Tự động tạo code từ name nếu code đang trống
        if (size.getCode() == null || size.getCode().isBlank()) {
            size.setCode(toSlug(size.getName()));
        }
        // ------------------------------------

        // 2. Kiểm tra trùng lặp (giữ nguyên logic cũ)
        Optional<Size> existingSize = sizeRepository.findByName(size.getName());
        if (existingSize.isPresent() && (size.getId() == null || !existingSize.get().getId().equals(size.getId()))) {
            throw new DataIntegrityViolationException("Tên kích thước '" + size.getName() + "' đã tồn tại.");
        }

        // 3. Tiến hành lưu
        sizeRepository.save(size);
    }

    public void deleteById(Long id) {
        sizeRepository.deleteById(id);
    }
}