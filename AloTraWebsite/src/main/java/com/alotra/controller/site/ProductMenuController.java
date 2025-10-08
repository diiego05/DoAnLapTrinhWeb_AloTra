package com.alotra.controller.site;

import com.alotra.dto.ProductSummaryDTO;
import com.alotra.entity.Category;
import com.alotra.service.CategoryService;
import com.alotra.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class ProductMenuController {

    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;

    @GetMapping("/menu")
    public String menuPage(
            @RequestParam(name = "category", required = false) String categorySlug,
            @RequestParam(name = "page", defaultValue = "1") int page, // Nhận số trang
            @RequestParam(name = "size", defaultValue = "8") int size,  // Nhận số lượng mỗi trang
            Model model) {

        // 1. Tạo đối tượng Pageable (lưu ý: page trong Pageable bắt đầu từ 0)
        Pageable pageable = PageRequest.of(page - 1, size);

        // 2. Lấy dữ liệu sản phẩm đã được phân trang
        Page<ProductSummaryDTO> productPage = productService.findActiveProducts(categorySlug, pageable);

        // 3. Lấy tất cả danh mục để hiển thị sidebar (giữ nguyên)
        List<Category> categories = categoryService.findAll();

        // 4. Lấy thông tin danh mục hiện tại (giữ nguyên)
        String currentCategoryName = "Tất cả sản phẩm";
        if (categorySlug != null && !categorySlug.isEmpty()) {
            currentCategoryName = categories.stream()
                .filter(c -> c.getSlug().equals(categorySlug))
                .map(Category::getName)
                .findFirst()
                .orElse("Tất cả sản phẩm");
        }

        // 5. Đưa dữ liệu ra Model
        model.addAttribute("productPage", productPage); // Gửi đối tượng Page ra view
        model.addAttribute("categories", categories);
        model.addAttribute("currentCategorySlug", categorySlug);
        model.addAttribute("currentCategoryName", currentCategoryName);

        // 6. Tạo danh sách các số trang để view có thể render
        int totalPages = productPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "products/menu"; // Đảm bảo đường dẫn đến file view là chính xác
    }
}