package com.alotra.service;

import com.alotra.dto.ProductDTO;
import com.alotra.dto.ProductFormDTO;
import com.alotra.dto.ProductListDTO;
import com.alotra.dto.ProductSummaryDTO;
import com.alotra.dto.ProductDetailDTO;
import com.alotra.dto.ProductVariantDTO;
import com.alotra.entity.*;
import com.alotra.repository.CategoryRepository;
import com.alotra.repository.ProductRepository;
import com.alotra.repository.SizeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.text.Normalizer;
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

    // ================= TRANG CHỦ =================
    @Transactional(readOnly = true)
    public List<ProductDTO> findBestSellers() {
        List<Product> products = productRepository.findAllWithDetails();
        return products.stream().map(this::convertToProductDTO).collect(Collectors.toList());
    }

    public Product findByVariantId(Long variantId) {
        // Nếu bạn có entity ProductVariant thì sửa lại logic bên dưới
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
        return dto;
    }
    public Product findById(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        return productOpt.orElse(null);
    }
    // ================= CREATE =================
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

    // ================= READ DETAIL =================
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

        dto.setVariants(product.getVariants().stream()
                .map(v -> new ProductVariantDTO(v.getId(), v.getSize().getId(), v.getPrice()))
                .collect(Collectors.toList()));

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
        product.setStatus(dto.getStatus()); // Cập nhật trạng thái
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));
        product.setCategory(category);

        // ✅ Xóa biến thể cũ khỏi DB
        product.getVariants().clear();
        productRepository.save(product);
        productRepository.flush();

        // Thêm lại biến thể mới
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

        // Ảnh
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


    // ================= DELETE =================
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

    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetail(Long id) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setStatus(p.getStatus());
        dto.setCategoryId(p.getCategory() != null ? p.getCategory().getId() : null);
        dto.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : null);
        dto.setImageUrls(p.getMedia().stream().map(ProductMedia::getUrl).toList());
        dto.setVariants(p.getVariants().stream().map(v -> {
            ProductVariantDTO var = new ProductVariantDTO();
            var.setSizeId(v.getSize().getId());
            var.setSizeName(v.getSize().getName());
            var.setPrice(v.getPrice());
            return var;
        }).toList());
        return dto;
    }


    /**
     * Lấy danh sách sản phẩm đang hoạt động, có thể lọc theo danh mục.
     * @param categorySlug Slug của danh mục (nếu null hoặc rỗng, sẽ lấy tất cả).
     * @return Danh sách ProductSummaryDTO.
     */
    public List<ProductSummaryDTO> findActiveProducts(String categorySlug) {
        List<Product> products;
        String activeStatus = "ACTIVE";

        if (categorySlug != null && !categorySlug.isEmpty()) {
            products = productRepository.findAllByStatusAndCategory_Slug(activeStatus, categorySlug);
        } else {
            products = productRepository.findAllByStatus(activeStatus);
        }

        // Chuyển đổi sang DTO để hiển thị
        return products.stream()
                .map(ProductSummaryDTO::new)
                .collect(Collectors.toList());
    }

    // SỬA LẠI: Nhận Pageable và trả về Page<ProductSummaryDTO>
    public Page<ProductSummaryDTO> findActiveProducts(String categorySlug, Pageable pageable) {
        Page<Product> productPage;
        String activeStatus = "ACTIVE";

        if (categorySlug != null && !categorySlug.isEmpty()) {
            productPage = productRepository.findAllByStatusAndCategory_Slug(activeStatus, categorySlug, pageable);
        } else {
            productPage = productRepository.findAllByStatus(activeStatus, pageable);
        }

        // Chuyển đổi Page<Product> sang Page<ProductSummaryDTO>
        return productPage.map(ProductSummaryDTO::new);
    }
}
