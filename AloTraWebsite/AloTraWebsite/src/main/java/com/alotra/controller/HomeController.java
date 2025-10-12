package com.alotra.controller;


import com.alotra.dto.ProductDTO; // Thêm import
import com.alotra.service.ProductService; // Thêm import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List; // Thêm import

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.alotra.dto.ProductDTO;

@Controller
public class HomeController {
	 @Autowired
	    private ProductService productService;
    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("pageTitle", "AloTra - Trang Chủ");
        // Sau này bạn có thể thêm dữ liệu sản phẩm nổi bật vào đây
     // Lấy danh sách sản phẩm bán chạy từ service
        List<ProductDTO> bestSellers = productService.findBestSellers();

        // Đưa danh sách vào model với tên là "bestSellers" để HTML có thể dùng
        model.addAttribute("bestSellers", bestSellers);
        return "home/index"; // Trả về tên file template (home/index.html)
    }

    // Tạm thời tạo các controller cơ bản cho các link trên header/footer để tránh lỗi 404
    @GetMapping("/products")
    public String productsPage(Model model) {
        model.addAttribute("pageTitle", "Sản Phẩm của AloTra");
        return "products/product_list"; // Sẽ tạo trang này sau
    }

    @GetMapping("/about")
    public String aboutPage(Model model) {
        model.addAttribute("pageTitle", "Về Chúng Tôi");
        return "about/about"; // Sẽ tạo trang này sau
    }

    @GetMapping("/contact")
    public String contactPage(Model model) {
        model.addAttribute("pageTitle", "Liên Hệ AloTra");
        return "contact/contact"; // Sẽ tạo trang này sau
    }



    @GetMapping("/cart")
    public String cartPage(Model model) {
        model.addAttribute("pageTitle", "Giỏ Hàng");
        return "cart/cart"; // Sẽ tạo trang này sau
    }

    @GetMapping("/promotion")
    public String showPromotionListPage() {
        return "promotions/index";  // trỏ đến templates/promotions/index.html
    }

    // 📰 Trang chi tiết khuyến mãi
    @GetMapping("/promotions/{id}")
    public String showPromotionDetailPage() {
        return "promotions/detail";  // trỏ đến templates/promotions/detail.html
    }



    @GetMapping("/profile")
    public String profilePage(Model model) {
        model.addAttribute("pageTitle", "Thông Tin Tài Khoản");
        return "user/profile"; // Sẽ tạo trang này sau
    }

    @GetMapping("/register-branch")
    public String registerBranchPage() {
        return "register-service/branch";
    }

    @GetMapping("/register-shipper")
    public String registerShipperPage() {
        return "register-service/shipper";
    }
}
