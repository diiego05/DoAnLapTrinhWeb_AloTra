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
    private BigDecimal lowestPrice;      // << BỔ SUNG
    private Long defaultVariantId;   // << BỔ SUNG

    public ProductSummaryDTO(Product product) {
        this.id = product.getId();
        this.slug = product.getSlug();
        this.name = product.getName();

        this.imageUrl = product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .map(ProductMedia::getUrl)
                .findFirst()
                .orElse(product.getMedia().stream().map(ProductMedia::getUrl).findFirst().orElse("/images/placeholder.png"));

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            // Tìm biến thể có giá thấp nhất
            Optional<ProductVariant> cheapestVariantOpt = product.getVariants().stream()
                    .min(Comparator.comparing(ProductVariant::getPrice));

            if (cheapestVariantOpt.isPresent()) {
                ProductVariant cheapestVariant = cheapestVariantOpt.get();
                this.lowestPrice = cheapestVariant.getPrice();
                this.defaultVariantId = cheapestVariant.getId();
            } else {
                this.lowestPrice = BigDecimal.ZERO;
            }

            // Tính toán khoảng giá (giữ lại nếu bạn vẫn muốn dùng ở đâu đó)
            BigDecimal maxPrice = product.getVariants().stream()
                    .map(ProductVariant::getPrice)
                    .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

            if (this.lowestPrice.compareTo(maxPrice) == 0) {
                this.priceRange = String.format("%,.0f đ", this.lowestPrice);
            } else {
                this.priceRange = String.format("%,.0f đ - %,.0f đ", this.lowestPrice, maxPrice);
            }
        } else {
            this.priceRange = "Liên hệ";
            this.lowestPrice = BigDecimal.ZERO;
        }
    }
}