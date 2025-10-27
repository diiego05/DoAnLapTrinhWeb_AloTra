/*package com.alotra.service;

import com.alotra.dto.*;
import com.alotra.entity.*;
import com.alotra.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private CloudinaryService cloudinaryService;

    @Autowired private PromotionTargetRepository promotionTargetRepository;
    @Autowired private PromotionalCampaignRepository promotionalCampaignRepository;

    // ================= TRANG CHỦ =================
    @Transactional(readOnly = true)
    public List<ProductDTO> findBestSellers() {
        List<Product> products = productRepository.findAllWithDetails();
        return products.stream().map(this::convertToProductDTO).collect(Collectors.toList());
    }

    public Product findByVariantId(Long variantId) {
        return productRepository.findProductByVariantId(variantId).orElse(null);
    }

    private ProductDTO convertToProductDTO(Product product) {
        String imageUrl = product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .map(ProductMedia::getUrl)
                .orElse("/images/placeholder.png");

        BigDecimal price = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new ProductDTO(product.getId(), product.getName(), imageUrl, price);
    }

    // ================= ADMIN LIST =================
    @Transactional(readOnly = true)
    public List<ProductListDTO> searchProductsForAdmin(String keyword) {
        List<Product> products = (keyword == null || keyword.isEmpty())
                ? productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                : productRepository.findByNameContainingIgnoreCase(keyword);

        return products.stream().map(this::convertToProductListDTO).collect(Collectors.toList());
    }

    private ProductListDTO convertToProductListDTO(Product product) {
        ProductListDTO dto = new ProductListDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setStatus(product.getStatus());

        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }

        product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .ifPresent(media -> dto.setPrimaryImageUrl(media.getUrl()));

        BigDecimal originalPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal discountedPrice = getDiscountedPrice(product);

        dto.setOriginalPrice(originalPrice);
        dto.setDiscountedPrice(discountedPrice);
        dto.setHasDiscount(discountedPrice.compareTo(originalPrice) < 0);

        if (dto.isHasDiscount()) {
            BigDecimal percent = BigDecimal.valueOf(100)
                    .subtract(discountedPrice.multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, BigDecimal.ROUND_HALF_UP));
            dto.setDiscountPercent(percent.intValue());
        } else {
            dto.setDiscountPercent(0);
        }

        return dto;
    }

    // ================= GIẢM GIÁ =================
    private BigDecimal getDiscountedPrice(Product product) {
        BigDecimal minPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<PromotionalCampaign> campaigns = promotionalCampaignRepository.findActiveCampaigns(LocalDateTime.now());
        BigDecimal bestPrice = minPrice;

        for (PromotionalCampaign campaign : campaigns) {
            boolean isTarget = promotionTargetRepository.findByProductId(product.getId())
                    .stream()
                    .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));

            if (isTarget) {
                BigDecimal discounted;
                BigDecimal value = campaign.getValue();

                switch (campaign.getType()) {
                    case ORDER_PERCENT:
                        discounted = minPrice.subtract(minPrice.multiply(value).divide(BigDecimal.valueOf(100)));
                        discounted = roundUpThousand(discounted);
                        break;
                    case ORDER_FIXED:
                        discounted = minPrice.subtract(value).max(BigDecimal.ZERO);
                        break;
                    default:
                        continue;
                }

                if (discounted.compareTo(bestPrice) < 0) {
                    bestPrice = discounted;
                }
            }
        }

        return bestPrice;
    }

    // ✅ Làm tròn lên 1.000đ
    private BigDecimal roundUpThousand(BigDecimal price) {
        BigDecimal thousand = new BigDecimal("1000");
        return price.divide(thousand, 0, RoundingMode.CEILING).multiply(thousand);
    }

    private void applyPromotion(ProductSummaryDTO dto, Product product) {
        BigDecimal originalPrice = dto.getLowestPrice();
        BigDecimal bestPrice = originalPrice;
        List<PromotionalCampaign> campaigns = promotionalCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        for (PromotionalCampaign campaign : campaigns) {
            boolean isTarget = promotionTargetRepository.findByProductId(product.getId())
                    .stream()
                    .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));

            if (isTarget) {
                BigDecimal discounted;
                BigDecimal value = campaign.getValue();

                switch (campaign.getType()) {
                    case ORDER_PERCENT:
                        discounted = originalPrice.subtract(originalPrice.multiply(value).divide(BigDecimal.valueOf(100)));
                        discounted = roundUpThousand(discounted);
                        break;
                    case ORDER_FIXED:
                        discounted = originalPrice.subtract(value).max(BigDecimal.ZERO);
                        break;
                    default:
                        continue;
                }

                if (discounted.compareTo(bestPrice) < 0) {
                    bestPrice = discounted;
                    dto.setHasDiscount(true);
                    dto.setOriginalPrice(originalPrice);
                    dto.setDiscountedPrice(bestPrice);

                    if (campaign.getType() == PromotionalCampaign.CampaignType.ORDER_PERCENT) {
                        dto.setDiscountPercent(value.intValue());
                    } else {
                        BigDecimal percent = BigDecimal.valueOf(100)
                                .subtract(bestPrice.multiply(BigDecimal.valueOf(100))
                                .divide(originalPrice, 0, BigDecimal.ROUND_HALF_UP));
                        dto.setDiscountPercent(percent.intValue());
                    }
                }
            }
        }
    }

    // ================= CRUD =================
    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    public Product createProduct(ProductFormDTO dto, List<MultipartFile> files) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setSlug(toSlug(dto.getName()));
        product.setDescription(dto.getDescription());
        product.setStatus("ACTIVE");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));
        product.setCategory(category);

        if (dto.getVariants() == null || dto.getVariants().isEmpty()) {
            throw new IllegalArgumentException("Sản phẩm phải có ít nhất một biến thể.");
        }

        dto.getVariants().forEach(variantDTO -> {
            Size size = sizeRepository.findById(variantDTO.getSizeId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy size!"));
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(size);
            variant.setPrice(variantDTO.getPrice());
            variant.setSku(product.getSlug() + "-" + size.getCode());
            product.getVariants().add(variant);
        });

        if (files != null && !files.isEmpty()) {
            final AtomicInteger counter = new AtomicInteger(0);
            files.forEach(file -> {
                String url = cloudinaryService.uploadFile(file);
                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setUrl(url);
                media.setPrimary(counter.getAndIncrement() == 0);
                product.getMedia().add(media);
            });
        }

        return productRepository.save(product);
    }

    // ================= 🆕 CHI TIẾT SẢN PHẨM + GIẢM GIÁ TỪNG SIZE =================
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetailById(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        dto.setStatus(product.getStatus());

        List<PromotionalCampaign> campaigns = promotionalCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        dto.setVariants(product.getVariants().stream().map(v -> {
            BigDecimal original = v.getPrice();
            BigDecimal bestPrice = original;
            int discountPercent = 0;
            boolean hasDiscount = false;

            for (PromotionalCampaign campaign : campaigns) {
                boolean isTarget = promotionTargetRepository.findByProductId(product.getId())
                        .stream()
                        .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));

                if (isTarget) {
                    BigDecimal discounted;
                    BigDecimal value = campaign.getValue();
                    switch (campaign.getType()) {
                        case ORDER_PERCENT:
                            discounted = original.subtract(original.multiply(value).divide(BigDecimal.valueOf(100)));
                            discounted = roundUpThousand(discounted);
                            discountPercent = value.intValue();
                            break;
                        case ORDER_FIXED:
                            discounted = original.subtract(value).max(BigDecimal.ZERO);
                            BigDecimal percent = BigDecimal.valueOf(100)
                                    .subtract(discounted.multiply(BigDecimal.valueOf(100))
                                    .divide(original, 0, BigDecimal.ROUND_HALF_UP));
                            discountPercent = percent.intValue();
                            break;
                        default:
                            continue;
                    }

                    if (discounted.compareTo(bestPrice) < 0) {
                        bestPrice = discounted;
                        hasDiscount = true;
                    }
                }
            }

            return new ProductVariantDTO(
                    v.getId(),
                    v.getSize().getId(),
                    v.getSize().getName(),
                    original,
                    bestPrice,
                    hasDiscount,
                    discountPercent,
                    v.getStatus()
            );
        }).collect(Collectors.toList()));

        dto.setImageUrls(product.getMedia().stream()
                .map(ProductMedia::getUrl)
                .collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public void updateProduct(Long id, ProductFormDTO dto, List<MultipartFile> files) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        product.setName(dto.getName());
        product.setSlug(toSlug(dto.getName()));
        product.setDescription(dto.getDescription());
        product.setStatus(dto.getStatus());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));
        product.setCategory(category);

        product.getVariants().clear();
        productRepository.save(product);
        productRepository.flush();

        dto.getVariants().forEach(variantDTO -> {
            Size size = sizeRepository.findById(variantDTO.getSizeId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy size!"));
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(size);
            variant.setPrice(variantDTO.getPrice());
            variant.setSku(product.getSlug() + "-" + size.getCode());
            variant.setStatus("ACTIVE");
            product.getVariants().add(variant);
        });

        if (files != null && !files.isEmpty()) {
            product.getMedia().clear();
            final AtomicInteger counter = new AtomicInteger(0);
            files.forEach(file -> {
                String url = cloudinaryService.uploadFile(file);
                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setUrl(url);
                media.setPrimary(counter.getAndIncrement() == 0);
                product.getMedia().add(media);
            });
        }

        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    // ================= UTILS =================
    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase().replace('đ', 'd');
    }

    // ================= FILTER ACTIVE =================
    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> findActiveProducts(String categorySlug) {
        List<Product> products;
        String activeStatus = "ACTIVE";

        if (categorySlug != null && !categorySlug.isEmpty()) {
            products = productRepository.findAllByStatusAndCategory_Slug(activeStatus, categorySlug);
        } else {
            products = productRepository.findAllByStatus(activeStatus);
        }

        return products.stream()
                .map(product -> {
                    ProductSummaryDTO dto = new ProductSummaryDTO(product);
                    applyPromotion(dto, product);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Page<ProductSummaryDTO> findActiveProducts(String categorySlug, Pageable pageable) {
        Page<Product> productPage;
        String activeStatus = "ACTIVE";

        if (categorySlug != null && !categorySlug.isEmpty()) {
            productPage = productRepository.findAllByStatusAndCategory_Slug(activeStatus, categorySlug, pageable);
        } else {
            productPage = productRepository.findAllByStatus(activeStatus, pageable);
        }

        return productPage.map(product -> {
            ProductSummaryDTO dto = new ProductSummaryDTO(product);
            applyPromotion(dto, product);
            return dto;
        });
    }
}*/
/*

package com.alotra.service;

import com.alotra.dto.*;
import com.alotra.entity.*;
import com.alotra.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private CloudinaryService cloudinaryService;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private BranchInventoryRepository branchInventoryRepository;
    @Autowired private PromotionTargetRepository promotionTargetRepository;
    @Autowired private PromotionalCampaignRepository promotionalCampaignRepository;

    // ================= TRANG CHỦ =================
    @Transactional(readOnly = true)
    public List<ProductDTO> findBestSellers() {
        List<Product> products = productRepository.findAllWithDetails();
        return products.stream().map(this::convertToProductDTO).collect(Collectors.toList());
    }

    public Product findByVariantId(Long variantId) {
        return productRepository.findProductByVariantId(variantId).orElse(null);
    }

    private ProductDTO convertToProductDTO(Product product) {
        String imageUrl = product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .map(ProductMedia::getUrl)
                .orElse("/images/placeholder.png");

        BigDecimal price = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new ProductDTO(product.getId(), product.getName(), imageUrl, price);
    }

    // ================= ADMIN LIST =================
    @Transactional(readOnly = true)
    public List<ProductListDTO> searchProductsForAdmin(String keyword) {
        List<Product> products = (keyword == null || keyword.isEmpty())
                ? productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                : productRepository.findByNameContainingIgnoreCase(keyword);

        return products.stream().map(this::convertToProductListDTO).collect(Collectors.toList());
    }

    private ProductListDTO convertToProductListDTO(Product product) {
        ProductListDTO dto = new ProductListDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setStatus(product.getStatus());

        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }

        product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .ifPresent(media -> dto.setPrimaryImageUrl(media.getUrl()));

        BigDecimal originalPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal discountedPrice = getDiscountedPrice(product);

        dto.setOriginalPrice(originalPrice);
        dto.setDiscountedPrice(discountedPrice);

        boolean hasDiscount = originalPrice.compareTo(BigDecimal.ZERO) > 0
                && discountedPrice.compareTo(originalPrice) < 0;
        dto.setHasDiscount(hasDiscount);

        if (hasDiscount) {
            // percent = round(100 - discounted/original * 100)
            BigDecimal percent = BigDecimal.valueOf(100)
                    .subtract(discountedPrice
                            .multiply(BigDecimal.valueOf(100))
                            .divide(originalPrice, 0, RoundingMode.HALF_UP));
            dto.setDiscountPercent(percent.intValue());
        } else {
            dto.setDiscountPercent(0);
        }

        return dto;
    }

    // ================= GIẢM GIÁ (mức giá tối ưu cho thẻ list) =================
    private BigDecimal getDiscountedPrice(Product product) {
        BigDecimal minPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (minPrice.compareTo(BigDecimal.ZERO) <= 0) return minPrice;

        List<PromotionalCampaign> campaigns = promotionalCampaignRepository.findActiveCampaigns(LocalDateTime.now());
        // preload targets 1 lần
        List<PromotionTarget> targets = promotionTargetRepository.findByProductId(product.getId());

        BigDecimal bestPrice = minPrice;

        for (PromotionalCampaign campaign : campaigns) {
            boolean isTarget = targets.stream()
                    .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));
            if (!isTarget) continue;

            BigDecimal rawDiscounted;
            BigDecimal finalDiscounted;

            switch (campaign.getType()) {
                case ORDER_PERCENT:
                    rawDiscounted = minPrice.subtract(
                            minPrice.multiply(campaign.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    );
                    // hiển thị làm tròn lên 1.000đ
                    finalDiscounted = roundUpThousand(rawDiscounted);
                    break;

                case ORDER_FIXED:
                    rawDiscounted = minPrice.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                    finalDiscounted = rawDiscounted;
                    break;

                default:
                    continue;
            }

            // phải thật sự rẻ hơn giá gốc (theo raw) và tốt hơn best hiện tại
            if (rawDiscounted.compareTo(minPrice) < 0 && finalDiscounted.compareTo(bestPrice) < 0) {
                bestPrice = finalDiscounted;
            }
        }

        return bestPrice;
    }

    // ✅ Làm tròn lên 1.000đ (cho loại giảm theo %)
    private BigDecimal roundUpThousand(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        BigDecimal thousand = new BigDecimal("1000");
        if (price.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return price.divide(thousand, 0, RoundingMode.CEILING).multiply(thousand);
    }

    private void applyPromotion(ProductSummaryDTO dto, Product product) {
        BigDecimal originalPrice = dto.getLowestPrice();
        if (originalPrice == null) originalPrice = BigDecimal.ZERO;

        BigDecimal bestPrice = originalPrice;
        boolean hasDiscount = false;
        int discountPercentShown = 0;

        List<PromotionalCampaign> campaigns = promotionalCampaignRepository.findActiveCampaigns(LocalDateTime.now());
        List<PromotionTarget> targets = promotionTargetRepository.findByProductId(product.getId());

        for (PromotionalCampaign campaign : campaigns) {
            boolean isTarget = targets.stream()
                    .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));
            if (!isTarget) continue;

            BigDecimal rawDiscounted;
            BigDecimal finalDiscounted;

            switch (campaign.getType()) {
                case ORDER_PERCENT:
                    rawDiscounted = originalPrice.subtract(
                            originalPrice.multiply(campaign.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    );
                    finalDiscounted = roundUpThousand(rawDiscounted);
                    break;

                case ORDER_FIXED:
                    rawDiscounted = originalPrice.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                    finalDiscounted = rawDiscounted;
                    break;

                default:
                    continue;
            }

            if (rawDiscounted.compareTo(originalPrice) < 0 && finalDiscounted.compareTo(bestPrice) < 0) {
                bestPrice = finalDiscounted;
                hasDiscount = true;

                if (campaign.getType() == PromotionalCampaign.CampaignType.ORDER_PERCENT) {
                    discountPercentShown = campaign.getValue().intValue();
                } else {
                    if (originalPrice.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal percent = BigDecimal.valueOf(100)
                                .subtract(bestPrice.multiply(BigDecimal.valueOf(100))
                                        .divide(originalPrice, 0, RoundingMode.HALF_UP));
                        discountPercentShown = percent.intValue();
                    } else {
                        discountPercentShown = 0;
                    }
                }
            }
        }

        dto.setHasDiscount(hasDiscount);
        dto.setOriginalPrice(originalPrice);
        dto.setDiscountedPrice(bestPrice);
        dto.setDiscountPercent(hasDiscount ? discountPercentShown : 0);
    }

    // ================= CRUD =================
    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    public Product createProduct(ProductFormDTO dto, List<MultipartFile> files) {
        // 1️⃣ Tạo đối tượng Product
        Product product = new Product();
        product.setName(dto.getName());
        product.setSlug(toSlug(dto.getName()));
        product.setDescription(dto.getDescription());
        product.setStatus("ACTIVE");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));
        product.setCategory(category);

        // 2️⃣ Kiểm tra biến thể
        if (dto.getVariants() == null || dto.getVariants().isEmpty()) {
            throw new IllegalArgumentException("Sản phẩm phải có ít nhất một biến thể.");
        }

        dto.getVariants().forEach(variantDTO -> {
            Size size = sizeRepository.findById(variantDTO.getSizeId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy size!"));
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(size);
            variant.setPrice(variantDTO.getPrice());
            variant.setSku(product.getSlug() + "-" + size.getCode());
            variant.setStatus("ACTIVE");
            product.getVariants().add(variant);
        });

        // 3️⃣ Xử lý media upload (nếu có)
        if (files != null && !files.isEmpty()) {
            final AtomicInteger counter = new AtomicInteger(0);
            files.forEach(file -> {
                String url = cloudinaryService.uploadFile(file);
                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setUrl(url);
                media.setPrimary(counter.getAndIncrement() == 0);
                product.getMedia().add(media);
            });
        }

        // 4️⃣ Lưu sản phẩm + variants + media
        Product savedProduct = productRepository.save(product);

        // 5️⃣ Đồng bộ với BranchInventory
        // Lấy tất cả chi nhánh
        List<Branch> branches = branchRepository.findAll();

        // Duyệt qua tất cả variants của product vừa tạo
        for (ProductVariant variant : savedProduct.getVariants()) {
            for (Branch branch : branches) {
                BranchInventory inventory = new BranchInventory();
                inventory.setBranchId(branch.getId());
                inventory.setVariantId(variant.getId());
                inventory.setStatus("AVAILABLE");
                branchInventoryRepository.save(inventory);
            }
        }

        return savedProduct;
    }


    // ================= 🆕 CHI TIẾT SẢN PHẨM + GIẢM GIÁ TỪNG SIZE =================
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetailById(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        dto.setStatus(product.getStatus());

        List<PromotionalCampaign> campaigns = promotionalCampaignRepository.findActiveCampaigns(LocalDateTime.now());
        List<PromotionTarget> targets = promotionTargetRepository.findByProductId(product.getId());

        dto.setVariants(product.getVariants().stream().map(v -> {
            BigDecimal original = v.getPrice();
            if (original == null) original = BigDecimal.ZERO;

            BigDecimal bestPrice = original;
            int discountPercent = 0;
            boolean hasDiscount = false;

            for (PromotionalCampaign campaign : campaigns) {
                boolean isTarget = targets.stream()
                        .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));
                if (!isTarget) continue;

                BigDecimal rawDiscounted;
                BigDecimal finalDiscounted;

                switch (campaign.getType()) {
                    case ORDER_PERCENT:
                        rawDiscounted = original.subtract(
                                original.multiply(campaign.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        );
                        finalDiscounted = roundUpThousand(rawDiscounted);
                        break;

                    case ORDER_FIXED:
                        rawDiscounted = original.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                        finalDiscounted = rawDiscounted;
                        break;

                    default:
                        continue;
                }

                if (rawDiscounted.compareTo(original) < 0 && finalDiscounted.compareTo(bestPrice) < 0) {
                    bestPrice = finalDiscounted;
                    hasDiscount = true;

                    if (campaign.getType() == PromotionalCampaign.CampaignType.ORDER_PERCENT) {
                        discountPercent = campaign.getValue().intValue();
                    } else {
                        if (original.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal percent = BigDecimal.valueOf(100)
                                    .subtract(bestPrice.multiply(BigDecimal.valueOf(100))
                                            .divide(original, 0, RoundingMode.HALF_UP));
                            discountPercent = percent.intValue();
                        } else {
                            discountPercent = 0;
                        }
                    }
                }
            }

            // Trả DTO an toàn bằng setters để tránh lệch constructor
            ProductVariantDTO pv = new ProductVariantDTO();
            pv.setId(v.getId());
            pv.setSizeId(v.getSize().getId());
            pv.setSizeName(v.getSize().getName());
            pv.setPrice(v.getPrice()); // giữ field price là giá gốc theo yêu cầu
            // các field mở rộng (nếu DTO đã thêm):
            try {
                // Dùng reflection-safe setter nếu đã có
                pv.getClass().getMethod("setOriginalPrice", BigDecimal.class).invoke(pv, original);
                pv.getClass().getMethod("setDiscountedPrice", BigDecimal.class).invoke(pv, hasDiscount ? bestPrice : original);
                pv.getClass().getMethod("setHasDiscount", boolean.class).invoke(pv, hasDiscount);
                pv.getClass().getMethod("setDiscountPercent", int.class).invoke(pv, hasDiscount ? discountPercent : 0);
            } catch (Exception ignore) {
                // Nếu DTO cũ chưa có các method này thì bỏ qua, FE sẽ đọc v.price
            }
            pv.setStatus(v.getStatus());

            return pv;
        }).collect(Collectors.toList()));

        dto.setImageUrls(product.getMedia().stream()
                .map(ProductMedia::getUrl)
                .collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public void updateProduct(Long id, ProductFormDTO dto, List<MultipartFile> files) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        // Cập nhật thông tin chung
        product.setName(dto.getName());
        product.setSlug(toSlug(dto.getName()));
        product.setDescription(dto.getDescription());
        product.setStatus(dto.getStatus());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));
        product.setCategory(category);

        // 🧹 Xóa các variant cũ & inventory tương ứng
        List<ProductVariant> oldVariants = productVariantRepository.findByProductId(id);
        for (ProductVariant oldVariant : oldVariants) {
            branchInventoryRepository.deleteByVariantId(oldVariant.getId());
        }
        product.getVariants().clear();
        productRepository.save(product);
        productRepository.flush();

        // 👉 Xác định trạng thái Variant dựa trên trạng thái Product
        String variantStatus = "ACTIVE".equalsIgnoreCase(product.getStatus()) ? "ACTIVE" : "INACTIVE";

        // 🆕 Thêm các variant mới
        dto.getVariants().forEach(variantDTO -> {
            Size size = sizeRepository.findById(variantDTO.getSizeId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy size!"));
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(size);
            variant.setPrice(variantDTO.getPrice());
            variant.setSku(product.getSlug() + "-" + size.getCode());
            variant.setStatus(variantStatus); // ✅ Gán trạng thái động
            product.getVariants().add(variant);
        });

        // 🖼️ Cập nhật ảnh
        if (files != null && !files.isEmpty()) {
            product.getMedia().clear();
            final AtomicInteger counter = new AtomicInteger(0);
            files.forEach(file -> {
                String url = cloudinaryService.uploadFile(file);
                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setUrl(url);
                media.setPrimary(counter.getAndIncrement() == 0);
                product.getMedia().add(media);
            });
        }

        // 💾 Lưu lại sản phẩm sau khi thêm variants mới
        Product updatedProduct = productRepository.save(product);

        // 🏪 Đồng bộ lại inventory cho tất cả chi nhánh
        List<Branch> branches = branchRepository.findAll();
        String inventoryStatus = "ACTIVE".equalsIgnoreCase(updatedProduct.getStatus()) ? "AVAILABLE" : "DISABLED";

        for (ProductVariant variant : updatedProduct.getVariants()) {
            for (Branch branch : branches) {
                BranchInventory inventory = new BranchInventory();
                inventory.setBranchId(branch.getId());
                inventory.setVariantId(variant.getId());
                inventory.setStatus(inventoryStatus);
                branchInventoryRepository.save(inventory);
            }
        }
    }


    @Transactional
    public void deleteProduct(Long id) {
        // Xóa inventory thủ công nếu database chưa có ON DELETE CASCADE
        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        for (ProductVariant variant : variants) {
            branchInventoryRepository.deleteByVariantId(variant.getId());
        }

        productRepository.deleteById(id);
    }

    // ================= UTILS =================
    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase().replace('đ', 'd');
    }

    // ================= FILTER ACTIVE =================
    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> findActiveProducts(String categorySlug) {
        List<Product> products;
        String activeStatus = "ACTIVE";

        if (categorySlug != null && !categorySlug.isEmpty()) {
            products = productRepository.findAllByStatusAndCategory_Slug(activeStatus, categorySlug);
        } else {
            products = productRepository.findAllByStatus(activeStatus);
        }

        return products.stream()
                .map(product -> {
                    ProductSummaryDTO dto = new ProductSummaryDTO(product);
                    applyPromotion(dto, product);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Page<ProductSummaryDTO> findActiveProducts(String categorySlug, Pageable pageable) {
        Page<Product> productPage;
        String activeStatus = "ACTIVE";

        if (categorySlug != null && !categorySlug.isEmpty()) {
            productPage = productRepository.findAllByStatusAndCategory_Slug(activeStatus, categorySlug, pageable);
        } else {
            productPage = productRepository.findAllByStatus(activeStatus, pageable);
        }

        return productPage.map(product -> {
            ProductSummaryDTO dto = new ProductSummaryDTO(product);
            applyPromotion(dto, product);
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> getTopDiscountProducts(int limit) {
        // Lấy toàn bộ sản phẩm đang ACTIVE
        List<Product> products = productRepository.findAllByStatus("ACTIVE");

        return products.stream()
                .map(product -> {
                    // Tạo DTO để lấy giá và giảm giá
                    ProductSummaryDTO dto = new ProductSummaryDTO(product);
                    applyPromotion(dto, product);
                    return dto;
                })
                // Chỉ lấy sản phẩm có giảm giá thực sự
                .filter(ProductSummaryDTO::isHasDiscount)
                // Sắp xếp theo phần trăm giảm giá giảm dần
                .sorted((a, b) -> Integer.compare(b.getDiscountPercent(), a.getDiscountPercent()))
                // Giới hạn top N
                .limit(limit)
                .collect(Collectors.toList());
    }

}*/
package com.alotra.service;

import com.alotra.dto.*;
import com.alotra.entity.*;
import com.alotra.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private CloudinaryService cloudinaryService;

    @Autowired private BranchRepository branchRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private BranchInventoryRepository branchInventoryRepository;

    @Autowired private PromotionTargetRepository promotionTargetRepository;
    @Autowired private PromotionalCampaignRepository promotionalCampaignRepository;

    // 🆕 Thêm repository để kiểm tra campaign có coupon không
    @Autowired private CouponRepository couponRepository;

    // ================= TRANG CHỦ =================
    @Transactional(readOnly = true)
    public List<ProductDTO> findBestSellers() {
        List<Product> products = productRepository.findAllWithDetails();
        return products.stream().map(this::convertToProductDTO).collect(Collectors.toList());
    }

    public Product findByVariantId(Long variantId) {
        return productRepository.findProductByVariantId(variantId).orElse(null);
    }

    private ProductDTO convertToProductDTO(Product product) {
        String imageUrl = product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .map(ProductMedia::getUrl)
                .orElse("/images/placeholder.png");

        BigDecimal price = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new ProductDTO(product.getId(), product.getName(), imageUrl, price);
    }

    // ================= ADMIN LIST =================
    @Transactional(readOnly = true)
    public List<ProductListDTO> searchProductsForAdmin(String keyword) {
        List<Product> products = (keyword == null || keyword.isEmpty())
                ? productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                : productRepository.findByNameContainingIgnoreCase(keyword);

        return products.stream().map(this::convertToProductListDTO).collect(Collectors.toList());
    }

    private ProductListDTO convertToProductListDTO(Product product) {
        ProductListDTO dto = new ProductListDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setStatus(product.getStatus());

        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }

        product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .ifPresent(media -> dto.setPrimaryImageUrl(media.getUrl()));

        BigDecimal originalPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal discountedPrice = getDiscountedPrice(product);

        dto.setOriginalPrice(originalPrice);
        dto.setDiscountedPrice(discountedPrice);

        boolean hasDiscount = originalPrice.compareTo(BigDecimal.ZERO) > 0
                && discountedPrice.compareTo(originalPrice) < 0;
        dto.setHasDiscount(hasDiscount);

        if (hasDiscount) {
            BigDecimal percent = BigDecimal.valueOf(100)
                    .subtract(discountedPrice
                            .multiply(BigDecimal.valueOf(100))
                            .divide(originalPrice, 0, RoundingMode.HALF_UP));
            dto.setDiscountPercent(percent.intValue());
        } else {
            dto.setDiscountPercent(0);
        }

        return dto;
    }

    // ================= GIẢM GIÁ (mức giá tối ưu cho thẻ list) =================
    private BigDecimal getDiscountedPrice(Product product) {
        BigDecimal minPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (minPrice.compareTo(BigDecimal.ZERO) <= 0) return minPrice;

        // Chỉ lấy các campaign active KHÔNG có coupon
        List<PromotionalCampaign> campaigns = promotionalCampaignRepository
                .findActiveCampaigns(LocalDateTime.now())
                .stream()
                .filter(c -> !couponRepository.existsByCampaignId(c.getId()))
                .collect(Collectors.toList());

        // preload targets 1 lần
        List<PromotionTarget> targets = promotionTargetRepository.findByProductId(product.getId());

        BigDecimal bestPrice = minPrice;

        for (PromotionalCampaign campaign : campaigns) {
            boolean isTarget = targets.stream()
                    .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));
            if (!isTarget) continue;

            BigDecimal rawDiscounted;
            BigDecimal finalDiscounted;

            switch (campaign.getType()) {
                case ORDER_PERCENT:
                    rawDiscounted = minPrice.subtract(
                            minPrice.multiply(campaign.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    );
                    // hiển thị làm tròn lên 1.000đ
                    finalDiscounted = roundUpThousand(rawDiscounted);
                    break;

                case ORDER_FIXED:
                    rawDiscounted = minPrice.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                    finalDiscounted = rawDiscounted;
                    break;

                default:
                    continue;
            }

            if (rawDiscounted.compareTo(minPrice) < 0 && finalDiscounted.compareTo(bestPrice) < 0) {
                bestPrice = finalDiscounted;
            }
        }

        return bestPrice;
    }

    // ✅ Làm tròn lên 1.000đ (cho loại giảm theo %)
    private BigDecimal roundUpThousand(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        BigDecimal thousand = new BigDecimal("1000");
        if (price.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return price.divide(thousand, 0, RoundingMode.CEILING).multiply(thousand);
    }

    private void applyPromotion(ProductSummaryDTO dto, Product product) {
        BigDecimal originalPrice = dto.getLowestPrice();
        if (originalPrice == null) originalPrice = BigDecimal.ZERO;

        BigDecimal bestPrice = originalPrice;
        boolean hasDiscount = false;
        int discountPercentShown = 0;

        // Chỉ lấy các campaign active KHÔNG có coupon
        List<PromotionalCampaign> campaigns = promotionalCampaignRepository
                .findActiveCampaigns(LocalDateTime.now())
                .stream()
                .filter(c -> !couponRepository.existsByCampaignId(c.getId()))
                .collect(Collectors.toList());

        List<PromotionTarget> targets = promotionTargetRepository.findByProductId(product.getId());

        for (PromotionalCampaign campaign : campaigns) {
            boolean isTarget = targets.stream()
                    .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));
            if (!isTarget) continue;

            BigDecimal rawDiscounted;
            BigDecimal finalDiscounted;

            switch (campaign.getType()) {
                case ORDER_PERCENT:
                    rawDiscounted = originalPrice.subtract(
                            originalPrice.multiply(campaign.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    );
                    finalDiscounted = roundUpThousand(rawDiscounted);
                    break;

                case ORDER_FIXED:
                    rawDiscounted = originalPrice.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                    finalDiscounted = rawDiscounted;
                    break;

                default:
                    continue;
            }

            if (rawDiscounted.compareTo(originalPrice) < 0 && finalDiscounted.compareTo(bestPrice) < 0) {
                bestPrice = finalDiscounted;
                hasDiscount = true;

                if (campaign.getType() == PromotionalCampaign.CampaignType.ORDER_PERCENT) {
                    discountPercentShown = campaign.getValue().intValue();
                } else {
                    if (originalPrice.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal percent = BigDecimal.valueOf(100)
                                .subtract(bestPrice.multiply(BigDecimal.valueOf(100))
                                        .divide(originalPrice, 0, RoundingMode.HALF_UP));
                        discountPercentShown = percent.intValue();
                    } else {
                        discountPercentShown = 0;
                    }
                }
            }
        }

        dto.setHasDiscount(hasDiscount);
        dto.setOriginalPrice(originalPrice);
        dto.setDiscountedPrice(bestPrice);
        dto.setDiscountPercent(hasDiscount ? discountPercentShown : 0);
    }

    // ================= CRUD =================
    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    public Product createProduct(ProductFormDTO dto, List<MultipartFile> files) {
        // 1️⃣ Tạo đối tượng Product
        Product product = new Product();
        product.setName(dto.getName());
        product.setSlug(toSlug(dto.getName()));
        product.setDescription(dto.getDescription());
        product.setStatus("ACTIVE");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));
        product.setCategory(category);

        // 2️⃣ Kiểm tra biến thể
        if (dto.getVariants() == null || dto.getVariants().isEmpty()) {
            throw new IllegalArgumentException("Sản phẩm phải có ít nhất một biến thể.");
        }

        dto.getVariants().forEach(variantDTO -> {
            Size size = sizeRepository.findById(variantDTO.getSizeId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy size!"));
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(size);
            variant.setPrice(variantDTO.getPrice());
            variant.setSku(product.getSlug() + "-" + size.getCode());
            variant.setStatus("ACTIVE");
            product.getVariants().add(variant);
        });

        // 3️⃣ Xử lý media upload (nếu có)
        if (files != null && !files.isEmpty()) {
            final AtomicInteger counter = new AtomicInteger(0);
            files.forEach(file -> {
                String url = cloudinaryService.uploadFile(file);
                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setUrl(url);
                media.setPrimary(counter.getAndIncrement() == 0);
                product.getMedia().add(media);
            });
        }

        // 4️⃣ Lưu sản phẩm + variants + media
        Product savedProduct = productRepository.save(product);

        // 5️⃣ Đồng bộ với BranchInventory
        List<Branch> branches = branchRepository.findAll();
        for (ProductVariant variant : savedProduct.getVariants()) {
            for (Branch branch : branches) {
                BranchInventory inventory = new BranchInventory();
                inventory.setBranchId(branch.getId());
                inventory.setVariantId(variant.getId());
                inventory.setStatus("AVAILABLE");
                branchInventoryRepository.save(inventory);
            }
        }

        return savedProduct;
    }

    // ================= 🆕 CHI TIẾT SẢN PHẨM + GIẢM GIÁ TỪNG SIZE =================
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetailById(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        dto.setStatus(product.getStatus());

        // Chỉ lấy các campaign active KHÔNG có coupon
        List<PromotionalCampaign> campaigns = promotionalCampaignRepository
                .findActiveCampaigns(LocalDateTime.now())
                .stream()
                .filter(c -> !couponRepository.existsByCampaignId(c.getId()))
                .collect(Collectors.toList());

        List<PromotionTarget> targets = promotionTargetRepository.findByProductId(product.getId());

        dto.setVariants(product.getVariants().stream().map(v -> {
            BigDecimal original = v.getPrice();
            if (original == null) original = BigDecimal.ZERO;

            BigDecimal bestPrice = original;
            int discountPercent = 0;
            boolean hasDiscount = false;

            for (PromotionalCampaign campaign : campaigns) {
                boolean isTarget = targets.stream()
                        .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));
                if (!isTarget) continue;

                BigDecimal rawDiscounted;
                BigDecimal finalDiscounted;

                switch (campaign.getType()) {
                    case ORDER_PERCENT:
                        rawDiscounted = original.subtract(
                                original.multiply(campaign.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        );
                        finalDiscounted = roundUpThousand(rawDiscounted);
                        break;

                    case ORDER_FIXED:
                        rawDiscounted = original.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                        finalDiscounted = rawDiscounted;
                        break;

                    default:
                        continue;
                }

                if (rawDiscounted.compareTo(original) < 0 && finalDiscounted.compareTo(bestPrice) < 0) {
                    bestPrice = finalDiscounted;
                    hasDiscount = true;

                    if (campaign.getType() == PromotionalCampaign.CampaignType.ORDER_PERCENT) {
                        discountPercent = campaign.getValue().intValue();
                    } else {
                        if (original.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal percent = BigDecimal.valueOf(100)
                                    .subtract(bestPrice.multiply(BigDecimal.valueOf(100))
                                            .divide(original, 0, RoundingMode.HALF_UP));
                            discountPercent = percent.intValue();
                        } else {
                            discountPercent = 0;
                        }
                    }
                }
            }

            ProductVariantDTO pv = new ProductVariantDTO();
            pv.setId(v.getId());
            pv.setSizeId(v.getSize().getId());
            pv.setSizeName(v.getSize().getName());
            pv.setPrice(v.getPrice()); // giữ field price là giá gốc
            try {
                pv.getClass().getMethod("setOriginalPrice", BigDecimal.class).invoke(pv, original);
                pv.getClass().getMethod("setDiscountedPrice", BigDecimal.class).invoke(pv, hasDiscount ? bestPrice : original);
                pv.getClass().getMethod("setHasDiscount", boolean.class).invoke(pv, hasDiscount);
                pv.getClass().getMethod("setDiscountPercent", int.class).invoke(pv, hasDiscount ? discountPercent : 0);
            } catch (Exception ignore) {}
            pv.setStatus(v.getStatus());

            return pv;
        }).collect(Collectors.toList()));

        dto.setImageUrls(product.getMedia().stream()
                .map(ProductMedia::getUrl)
                .collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public void updateProduct(Long id, ProductFormDTO dto, List<MultipartFile> files) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        // Cập nhật thông tin chung
        product.setName(dto.getName());
        product.setSlug(toSlug(dto.getName()));
        product.setDescription(dto.getDescription());
        product.setStatus(dto.getStatus());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));
        product.setCategory(category);

        // 🧹 Xóa các variant cũ & inventory tương ứng
        List<ProductVariant> oldVariants = productVariantRepository.findByProductId(id);
        for (ProductVariant oldVariant : oldVariants) {
            branchInventoryRepository.deleteByVariantId(oldVariant.getId());
        }
        product.getVariants().clear();
        productRepository.save(product);
        productRepository.flush();

        // 👉 Xác định trạng thái Variant dựa trên trạng thái Product
        String variantStatus = "ACTIVE".equalsIgnoreCase(product.getStatus()) ? "ACTIVE" : "INACTIVE";

        // 🆕 Thêm các variant mới
        dto.getVariants().forEach(variantDTO -> {
            Size size = sizeRepository.findById(variantDTO.getSizeId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy size!"));
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(size);
            variant.setPrice(variantDTO.getPrice());
            variant.setSku(product.getSlug() + "-" + size.getCode());
            variant.setStatus(variantStatus);
            product.getVariants().add(variant);
        });

        // 🖼️ Cập nhật ảnh
        if (files != null && !files.isEmpty()) {
            product.getMedia().clear();
            final AtomicInteger counter = new AtomicInteger(0);
            files.forEach(file -> {
                String url = cloudinaryService.uploadFile(file);
                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setUrl(url);
                media.setPrimary(counter.getAndIncrement() == 0);
                product.getMedia().add(media);
            });
        }

        // 💾 Lưu lại
        Product updatedProduct = productRepository.save(product);

        // 🏪 Đồng bộ inventory cho tất cả chi nhánh
        List<Branch> branches = branchRepository.findAll();
        String inventoryStatus = "ACTIVE".equalsIgnoreCase(updatedProduct.getStatus()) ? "AVAILABLE" : "DISABLED";

        for (ProductVariant variant : updatedProduct.getVariants()) {
            for (Branch branch : branches) {
                BranchInventory inventory = new BranchInventory();
                inventory.setBranchId(branch.getId());
                inventory.setVariantId(variant.getId());
                inventory.setStatus(inventoryStatus);
                branchInventoryRepository.save(inventory);
            }
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        // Xóa inventory thủ công nếu database chưa có ON DELETE CASCADE
        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        for (ProductVariant variant : variants) {
            branchInventoryRepository.deleteByVariantId(variant.getId());
        }

        productRepository.deleteById(id);
    }

    // ================= UTILS =================
    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase().replace('đ', 'd');
    }

    // ================= FILTER ACTIVE =================
    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> findActiveProducts(String categorySlug) {
        List<Product> products;
        String activeStatus = "ACTIVE";

        if (categorySlug != null && !categorySlug.isEmpty()) {
            products = productRepository.findAllByStatusAndCategory_Slug(activeStatus, categorySlug);
        } else {
            products = productRepository.findAllByStatus(activeStatus);
        }

        return products.stream()
                .map(product -> {
                    ProductSummaryDTO dto = new ProductSummaryDTO(product);
                    applyPromotion(dto, product);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Page<ProductSummaryDTO> findActiveProducts(String categorySlug, Pageable pageable) {
        Page<Product> productPage;
        String activeStatus = "ACTIVE";

        if (categorySlug != null && !categorySlug.isEmpty()) {
            productPage = productRepository.findAllByStatusAndCategory_Slug(activeStatus, categorySlug, pageable);
        } else {
            productPage = productRepository.findAllByStatus(activeStatus, pageable);
        }

        return productPage.map(product -> {
            ProductSummaryDTO dto = new ProductSummaryDTO(product);
            applyPromotion(dto, product);
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> getTopDiscountProducts(int limit) {
        List<Product> products = productRepository.findAllByStatus("ACTIVE");

        return products.stream()
                .map(product -> {
                    ProductSummaryDTO dto = new ProductSummaryDTO(product);
                    applyPromotion(dto, product); // đã filter campaign không coupon bên trong
                    return dto;
                })
                .filter(ProductSummaryDTO::isHasDiscount)
                .sorted((a, b) -> Integer.compare(b.getDiscountPercent(), a.getDiscountPercent()))
                .limit(limit)
                .collect(Collectors.toList());
    }


    @Transactional
    public void updateProductForBranch(Long id, ProductFormDTO dto, List<MultipartFile> files, Long branchId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        // ✅ Cập nhật thông tin chung (nếu bạn muốn cho phép)
        product.setName(dto.getName());
        product.setSlug(toSlug(dto.getName()));
        product.setDescription(dto.getDescription());
        product.setStatus(dto.getStatus());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));
        product.setCategory(category);

        // 🧹 Xóa các variant cũ của sản phẩm (trong DB)
        List<ProductVariant> oldVariants = productVariantRepository.findByProductId(id);

        // ⚠️ Xóa inventory chỉ của chi nhánh hiện tại (không đụng chi nhánh khác)
        for (ProductVariant oldVariant : oldVariants) {
            branchInventoryRepository.deleteByVariantIdAndBranchId(oldVariant.getId(), branchId);
        }

        product.getVariants().clear();
        productRepository.save(product);
        productRepository.flush();

        String variantStatus = "ACTIVE".equalsIgnoreCase(product.getStatus()) ? "ACTIVE" : "INACTIVE";

        // 🆕 Thêm variant mới
        dto.getVariants().forEach(variantDTO -> {
            Size size = sizeRepository.findById(variantDTO.getSizeId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy size!"));
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(size);
            variant.setPrice(variantDTO.getPrice());
            variant.setSku(product.getSlug() + "-" + size.getCode());
            variant.setStatus(variantStatus);
            product.getVariants().add(variant);
        });

        // 🖼️ Cập nhật media (nếu cho phép theo chi nhánh)
        if (files != null && !files.isEmpty()) {
            product.getMedia().clear();
            final AtomicInteger counter = new AtomicInteger(0);
            files.forEach(file -> {
                String url = cloudinaryService.uploadFile(file);
                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setUrl(url);
                media.setPrimary(counter.getAndIncrement() == 0);
                product.getMedia().add(media);
            });
        }

        Product updatedProduct = productRepository.save(product);

        // 🏪 Cập nhật inventory chỉ cho branchId này
        String inventoryStatus = "ACTIVE".equalsIgnoreCase(updatedProduct.getStatus()) ? "AVAILABLE" : "DISABLED";

        for (ProductVariant variant : updatedProduct.getVariants()) {
            BranchInventory inventory = new BranchInventory();
            inventory.setBranchId(branchId);
            inventory.setVariantId(variant.getId());
            inventory.setStatus(inventoryStatus);
            branchInventoryRepository.save(inventory);
        }
    }
    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> getTopBestSellers(int limit) {
        List<Object[]> salesData = productRepository.findProductSalesCounts();

        if (salesData == null || salesData.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> salesMap = salesData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        List<Product> products = productRepository.findAllByStatus("ACTIVE");

        return products.stream()
                .filter(p -> salesMap.containsKey(p.getId()))
                .map(product -> {
                    ProductSummaryDTO dto = new ProductSummaryDTO(product);
                    applyPromotion(dto, product);
                    dto.setSoldCount(salesMap.getOrDefault(product.getId(), 0L));
                    return dto;
                })
                .sorted((a, b) -> Long.compare(b.getSoldCount(), a.getSoldCount()))
                .limit(limit)
                .toList();
    }




}

