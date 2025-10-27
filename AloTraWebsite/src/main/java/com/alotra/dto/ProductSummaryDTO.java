package com.alotra.dto;

import com.alotra.entity.Product;
import com.alotra.entity.ProductMedia;
import com.alotra.entity.ProductVariant;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;

@Data
@NoArgsConstructor
public class ProductSummaryDTO {

    private Long id;
    private String slug;
    private String name;
    private String imageUrl;
    private String priceRange;
    private BigDecimal lowestPrice;          // Giá thấp nhất
    private Long defaultVariantId;           // ID của biến thể rẻ nhất
    
    // ✅ Thêm categoryId để lọc sản phẩm theo danh mục
    private Long categoryId;

    // ✅ Các trường phục vụ giảm giá
    private boolean hasDiscount;             // Có giảm giá hay không
    private BigDecimal originalPrice;        // Giá gốc
    private BigDecimal discountedPrice;      // Giá sau giảm
    private int discountPercent;             // % giảm
    private long soldCount;
    
    public ProductSummaryDTO(Product product) {
        this.id = product.getId();
        this.slug = product.getSlug();
        this.name = product.getName();
        
        // ✅ Lưu categoryId từ product
        this.categoryId = product.getCategory() != null ? product.getCategory().getId() : null;

        // Ảnh đại diện
        this.imageUrl = product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .map(ProductMedia::getUrl)
                .findFirst()
                .orElse(product.getMedia().stream()
                        .map(ProductMedia::getUrl)
                        .findFirst()
                        .orElse("/images/placeholder.png"));

        // Giá thấp nhất & khoảng giá
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            Optional<ProductVariant> cheapestVariantOpt = product.getVariants().stream()
                    .min(Comparator.comparing(ProductVariant::getPrice));

            if (cheapestVariantOpt.isPresent()) {
                ProductVariant cheapestVariant = cheapestVariantOpt.get();
                this.lowestPrice = cheapestVariant.getPrice();
                this.defaultVariantId = cheapestVariant.getId();
            } else {
                this.lowestPrice = BigDecimal.ZERO;
            }

            BigDecimal maxPrice = product.getVariants().stream()
                    .map(ProductVariant::getPrice)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            if (this.lowestPrice.compareTo(maxPrice) == 0) {
                this.priceRange = String.format("%,.0f đ", this.lowestPrice);
            } else {
                this.priceRange = String.format("%,.0f đ - %,.0f đ", this.lowestPrice, maxPrice);
            }
        } else {
            this.priceRange = "Liên hệ";
            this.lowestPrice = BigDecimal.ZERO;
        }

        // ✅ Mặc định giá gốc ban đầu = lowestPrice
        this.originalPrice = this.lowestPrice;
        this.discountedPrice = this.lowestPrice;
        this.hasDiscount = false;
        this.discountPercent = 0;
    }
}