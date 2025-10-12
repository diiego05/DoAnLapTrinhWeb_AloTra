package com.alotra.service;

import com.alotra.dto.ProductVariantDTO;
import com.alotra.entity.ProductVariant;
import com.alotra.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;

    public ProductVariantService(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    // 🟢 Trả về entity (nếu nội bộ service cần dùng)
    public List<ProductVariant> getActiveVariantsByProductId(Long productId) {
        return variantRepository.findByProduct_IdAndStatus(productId, "ACTIVE");
    }

    public ProductVariant getById(Long id) {
        return variantRepository.findById(id).orElse(null);
    }

    // 🟢 Trả về DTO để controller trả JSON gọn sạch (không vướng proxy)
    public List<ProductVariantDTO> getVariantDTOsByProductId(Long productId) {
        List<ProductVariant> variants = getActiveVariantsByProductId(productId);
        return variants.stream()
                .map(v -> new ProductVariantDTO(
                        v.getId(),
                        v.getSize().getId(),
                        v.getSize().getName(),
                        v.getPrice(),
                        v.getStatus()
                ))
                .collect(Collectors.toList());
    }
}
