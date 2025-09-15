// 📁 com/alotra/service/ProductService.java
package com.alotra.service;

import com.alotra.dto.ProductDTO;
import com.alotra.entity.Product;
import com.alotra.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<ProductDTO> findBestSellers() {
        // Tạm thời lấy tất cả sản phẩm làm sản phẩm bán chạy
        List<Product> products = productRepository.findAll();

        return products.stream().map(product -> {
            // Lấy ảnh đại diện (isPrimary = true)
            String imageUrl = product.getMedia().stream()
                .filter(media -> media.isPrimary())
                .findFirst()
                .map(media -> media.getUrl())
                .orElse("/images/placeholder.png"); // Ảnh mặc định nếu không có

            // Lấy giá thấp nhất từ các biến thể size
            BigDecimal price = product.getVariants().stream()
                .map(variant -> variant.getPrice())
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO); // Giá mặc định nếu chưa có size/giá

            return new ProductDTO(product.getId(), product.getName(), imageUrl, price);
        }).collect(Collectors.toList());
    }
}