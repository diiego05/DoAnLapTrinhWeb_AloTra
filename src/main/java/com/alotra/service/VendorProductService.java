package com.alotra.service;

import com.alotra.dto.ProductDetailDTO;
import com.alotra.dto.VendorProductSummaryDTO;
import com.alotra.entity.BranchInventoryId;
import com.alotra.entity.Product;
import com.alotra.entity.ProductVariant;
import com.alotra.repository.BranchInventoryRepository;
import com.alotra.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorProductService {

    private final ProductRepository productRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final ProductService productService;
    private final BranchService branchService;

    /**
     * 📍 Lấy branchId mà vendor hiện tại đang quản lý
     */
    public Long getBranchIdByManagerId(Long vendorId) {
        return branchService.getBranchIdByVendorId(vendorId);
    }

    /**
     * 🧾 Lấy danh sách sản phẩm theo chi nhánh của vendor
     */
    @Transactional(readOnly = true)
    public List<VendorProductSummaryDTO> getProductsForBranch(Long branchId) {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(product -> product.getVariants().stream().anyMatch(
                        variant -> branchInventoryRepository.existsByBranchIdAndVariantIdAndStatus(branchId, variant.getId(), "AVAILABLE") ||
                                branchInventoryRepository.existsByBranchIdAndVariantIdAndStatus(branchId, variant.getId(), "DISABLED")
                ))
                .map(product -> toVendorProductDTO(product, branchId))
                .collect(Collectors.toList());
    }

    /**
     * 🧠 Hàm chuyển Product → VendorProductSummaryDTO
     */
    private VendorProductSummaryDTO toVendorProductDTO(Product product, Long branchId) {
        // ✅ Lấy ảnh đầu tiên (ưu tiên ảnh primary)
        String imageUrl = product.getMedia().stream()
                .sorted(Comparator.comparing(m -> !m.isPrimary()))
                .map(m -> m.getUrl())
                .findFirst()
                .orElse(null);

        // ✅ Tính giá từ variants
        var sortedPrices = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .filter(price -> price != null)
                .sorted()
                .toList();

        String priceRange = "";
        Double lowestPrice = null;
        Long defaultVariantId = null;

        if (!sortedPrices.isEmpty()) {
            BigDecimal min = sortedPrices.get(0);
            BigDecimal max = sortedPrices.get(sortedPrices.size() - 1);

            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            if (min.equals(max)) {
                priceRange = formatter.format(min) + " đ";
            } else {
                priceRange = formatter.format(min) + " đ - " + formatter.format(max) + " đ";
            }

            lowestPrice = min.doubleValue();
            defaultVariantId = product.getVariants().stream()
                    .filter(v -> v.getPrice().equals(min))
                    .map(ProductVariant::getId)
                    .findFirst()
                    .orElse(null);
        }

        // ✅ Mặc định chưa có khuyến mãi
        boolean hasDiscount = false;
        Double originalPrice = lowestPrice;
        Double discountedPrice = lowestPrice;
        int discountPercent = 0;

        // ✅ Kiểm tra trạng thái tồn kho
        boolean hasAvailable = product.getVariants().stream()
                .anyMatch(v -> branchInventoryRepository.existsByBranchIdAndVariantIdAndStatus(branchId, v.getId(), "AVAILABLE"));

        boolean hasDisabled = product.getVariants().stream()
                .anyMatch(v -> branchInventoryRepository.existsByBranchIdAndVariantIdAndStatus(branchId, v.getId(), "DISABLED"));

        String branchInventoryStatus = hasAvailable ? "AVAILABLE" : (hasDisabled ? "DISABLED" : "NONE");

        return new VendorProductSummaryDTO(
                product.getId(),
                product.getSlug(),
                product.getName(),
                imageUrl,
                priceRange,
                lowestPrice,
                defaultVariantId,
                hasDiscount,
                originalPrice,
                discountedPrice,
                discountPercent,
                product.getStatus(),
                branchInventoryStatus
        );
    }

    /**
     * 🔍 Lấy chi tiết sản phẩm theo chi nhánh
     */
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetailForBranch(Long productId, Long branchId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return null;

        boolean hasAccess = product.getVariants().stream()
                .anyMatch(v -> branchInventoryRepository.existsByBranchIdAndVariantIdAndStatus(branchId, v.getId(), "AVAILABLE") ||
                        branchInventoryRepository.existsByBranchIdAndVariantIdAndStatus(branchId, v.getId(), "DISABLED"));

        if (!hasAccess) return null;
        return productService.getProductDetailById(productId);
    }

    /**
     * 🔄 Cập nhật trạng thái sản phẩm theo chi nhánh
     */
    @Transactional
    public void updateProductStatusForBranch(Long productId, Long branchId, String status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        String inventoryStatus = "ACTIVE".equalsIgnoreCase(status) ? "AVAILABLE" : "DISABLED";

        for (ProductVariant variant : product.getVariants()) {
            var id = new BranchInventoryId(branchId, variant.getId());
            branchInventoryRepository.findById(id).ifPresent(inventory -> {
                inventory.setStatus(inventoryStatus);
                branchInventoryRepository.save(inventory);
            });
        }
    }
}
