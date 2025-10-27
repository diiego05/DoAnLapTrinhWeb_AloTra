package com.alotra.controller;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import com.alotra.entity.Address;
import com.alotra.entity.Branch;
import com.alotra.entity.Category;
import com.alotra.entity.Role;
import com.alotra.entity.Size;
import com.alotra.entity.Topping;
import com.alotra.entity.User;
import com.alotra.repository.UserRepository;
import com.alotra.service.BranchService;
import com.alotra.service.CategoryService;
import com.alotra.service.SizeService;
import com.alotra.service.UserService;
import com.alotra.service.ToppingService;

import jakarta.validation.Valid;
@Controller
@RequestMapping("/admin")
public class AdminController {

	 @Autowired
	    private BranchService branchService;

	    @Autowired
	    private UserService userService;

	    @Autowired
	    private UserRepository userRepository;
	    @Autowired
	    private CategoryService categoryService;
	    @Autowired
	    private ToppingService toppingService;
	  //... các @Autowired khác
	    @Autowired
	    private SizeService sizeService;
	    @GetMapping

	    public String showAdminRoot() {
	        return "redirect:/admin/dashboard";
	    }

	    @GetMapping("/dashboard")
	    public String showDashboard(Model model) {
	        model.addAttribute("pageTitle", "Tổng quan");
	        model.addAttribute("currentPage", "dashboard");
	        return "admin/dashboard";
	    }

	    // --- QUẢN LÝ NGƯỜI DÙNG ---

	    @GetMapping("/users")
	    public String showUserPage(Model model,
	                               @RequestParam(required = false) String keyword,
	                               @RequestParam(required = false) Long roleId,
	                               @RequestParam(required = false) String status) {
	        List<User> userList = userService.searchAndFilter(keyword, roleId, status);
	        List<Role> roleList = userService.findAllRoles();

	        model.addAttribute("userList", userList);
	        model.addAttribute("roleList", roleList);
	        model.addAttribute("keyword", keyword);
	        model.addAttribute("roleId", roleId);
	        model.addAttribute("status", status);

	        model.addAttribute("pageTitle", "Quản lý Người dùng");
	        model.addAttribute("currentPage", "users");
	        return "admin/users";
	    }

	    @GetMapping("/shipper-requests")
	    public String shipperRequestsPage(Model model) {
	        model.addAttribute("currentPage", "shipper-requests");
	        model.addAttribute("pageTitle", "Yêu cầu Shipper");
	        return "admin/admin-shipper-requests";
	    }

	    @GetMapping("/branch-requests")
	    public String branchRequestsPage(Model model) {
	        model.addAttribute("currentPage", "branch-requests");
	        model.addAttribute("pageTitle", "Yêu cầu Chi nhánh");
	        return "admin/admin-branch-requests";
	    }
	    @GetMapping("/users/add")
	    public String showAddUserForm(Model model) {
	        model.addAttribute("user", new User());
	        model.addAttribute("roleList", userService.findAllRoles());

	        model.addAttribute("pageTitle", "Thêm Mới");
	        model.addAttribute("parentPageTitle", "Quản lý Người dùng");
	        model.addAttribute("parentPageUrl", "/admin/users");

	        model.addAttribute("currentPage", "users");
	        return "admin/user-form";
	    }
/*
	    @GetMapping("/users/edit/{id}")
	    public String showEditUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
	        User user = userService.findById(id);
	        if (user == null) {
	            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
	            return "redirect:/admin/users";
	        }
	        model.addAttribute("user", user);
	        model.addAttribute("roleList", userService.findAllRoles());

	        model.addAttribute("pageTitle", "Chỉnh Sửa");
	        model.addAttribute("parentPageTitle", "Quản lý Người dùng");
	        model.addAttribute("parentPageUrl", "/admin/users");

	        model.addAttribute("currentPage", "users");
	        return "admin/user-form";
	    }
*/
	    @GetMapping("/users/edit/{id}")
	    public String showEditUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
	        User user = userService.findById(id);
	        if (user == null) {
	            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
	            return "redirect:/admin/users";
	        }

	        model.addAttribute("roleList", userService.findAllRoles());
	        model.addAttribute("user", user);

	        Address defaultAddress = user.getAddresses()
                    .stream()
                    .filter(Address::isDefault)  // ✅ đúng getter của boolean
                    .findFirst()
                    .orElse(null);

	        model.addAttribute("defaultAddress", defaultAddress);

	        model.addAttribute("pageTitle", "Chỉnh Sửa");
	        model.addAttribute("parentPageTitle", "Quản lý Người dùng");
	        model.addAttribute("parentPageUrl", "/admin/users");
	        model.addAttribute("currentPage", "users");

	        return "admin/user-form";
	    }


	 // Vị trí: trong file com.alotra.controller.AdminController.java

	    @PostMapping("/users/save")
	    public String saveUser(@Valid @ModelAttribute("user") User user,
	                           BindingResult bindingResult,
	                           @RequestParam("avatarFile") MultipartFile avatarFile,
	                           @RequestParam(required = false) String addressLine1,
	                           @RequestParam(required = false) String addressCity,
	                           @RequestParam(required = false) String addressWard,
	                           Model model, RedirectAttributes redirectAttributes) {

	        // Yêu cầu mật khẩu khi tạo mới
	        if (user.getId() == null && !StringUtils.hasText(user.getRawPassword())) {
	            bindingResult.rejectValue("rawPassword", "error.user", "Mật khẩu là bắt buộc khi tạo mới");
	        }

	        // Nếu có lỗi validation ban đầu, trả về ngay
	        if (bindingResult.hasErrors()) {
	            model.addAttribute("roleList", userService.findAllRoles());
	            model.addAttribute("pageTitle", user.getId() == null ? "Thêm Mới" : "Chỉnh Sửa");
	            model.addAttribute("parentPageTitle", "Quản lý Người dùng");
	            model.addAttribute("parentPageUrl", "/admin/users");
	            return "admin/user-form";
	        }

	        try {
	            userService.save(user, avatarFile, addressLine1, addressCity, addressWard);
	            redirectAttributes.addFlashAttribute("message", "Lưu người dùng thành công!");

	        } catch (DataIntegrityViolationException e) {
	            // --- PHẦN XỬ LÝ LỖI TRÙNG LẶP ---
	            String errorMessage = e.getMostSpecificCause().getMessage();

	            // Kiểm tra xem lỗi là do email, phone hay cccd
	            if (errorMessage.contains("UQ__Users__A9D105342503B089") || errorMessage.toLowerCase().contains("email")) {
	                bindingResult.rejectValue("email", "error.user", "Email này đã được sử dụng.");
	            } else if (errorMessage.contains("UQ__Users__5C7E359EB190CE36") || errorMessage.toLowerCase().contains("phone")) {
	                bindingResult.rejectValue("phone", "error.user", "Số điện thoại này đã được sử dụng.");
	            } else if (errorMessage.contains("UQ__Users__713A7B91E8BBD828") || errorMessage.toLowerCase().contains("idcardnumber")) {
	                 bindingResult.rejectValue("idCardNumber", "error.user", "Số CCCD này đã được sử dụng.");
	            } else {
	                // Lỗi trùng lặp chung
	                model.addAttribute("error", "Lỗi: Dữ liệu bị trùng lặp, vui lòng kiểm tra lại.");
	            }

	            // Chuẩn bị dữ liệu để hiển thị lại form với lỗi đỏ
	            model.addAttribute("roleList", userService.findAllRoles());
	            model.addAttribute("pageTitle", user.getId() == null ? "Thêm Mới" : "Chỉnh Sửa");
	            model.addAttribute("parentPageTitle", "Quản lý Người dùng");
	            model.addAttribute("parentPageUrl", "/admin/users");
	            return "admin/user-form";
	        }

	        return "redirect:/admin/users";
	    }
	    @GetMapping("/users/delete/{id}")
	    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
	        try {
	            userService.deleteById(id);
	            redirectAttributes.addFlashAttribute("message", "Xóa người dùng thành công!");
	        } catch (Exception e) {
	            redirectAttributes.addFlashAttribute("error", "Lỗi: Không thể xóa người dùng này.");
	        }
	        return "redirect:/admin/users";
	    }
	 // HIỂN THỊ TRANG CHI TIẾT NGƯỜI DÙNG (Đã cập nhật)
	    @GetMapping("/users/details/{id}")
	    public String showUserDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
	        User user = userService.findById(id);
	        if (user == null) {
	            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
	            return "redirect:/admin/users";
	        }

	        // --- BREADCRUMB (Sử dụng pageTitle) ---
	        model.addAttribute("pageTitle", "Chi tiết");
	        model.addAttribute("parentPageTitle", "Quản lý Người dùng");
	        model.addAttribute("parentPageUrl", "/admin/users");
	        // ------------------------------------

	        model.addAttribute("user", user);
	        model.addAttribute("currentPage", "users");
	        return "admin/user-details"; // Trỏ đến view mới
	    }
	    // --- QUẢN LÝ CHI NHÁNH ---

	    @GetMapping("/branches")
	    public String showBranchPage(Model model,
	                                 @RequestParam(required = false) String keyword,
	                                 @RequestParam(required = false) String status) {
	        List<Branch> branchList = branchService.searchByKeywordAndStatus(keyword, status);
	        model.addAttribute("branchList", branchList);
	        model.addAttribute("keyword", keyword);
	        model.addAttribute("status", status);

	        model.addAttribute("pageTitle", "Quản lý Cửa hàng");
	        model.addAttribute("currentPage", "branches");
	        return "admin/branches";
	    }

	    @GetMapping("/branches/add")
	    public String showAddBranchForm(Model model) {
	        model.addAttribute("branch", new Branch());
	        model.addAttribute("users", userRepository.findAll());

	        model.addAttribute("pageTitle", "Thêm Mới");
	        model.addAttribute("parentPageTitle", "Quản lý Cửa hàng");
	        model.addAttribute("parentPageUrl", "/admin/branches");

	        model.addAttribute("currentPage", "branches");
	        return "admin/branch-form";
	    }

	    @GetMapping("/branches/edit/{id}")
	    public String showEditBranchForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
	        Branch branch = branchService.findById(id);
	        if (branch == null) {
	            redirectAttributes.addFlashAttribute("error", "Lỗi: Không tìm thấy chi nhánh có ID = " + id);
	            return "redirect:/admin/branches";
	        }
	        model.addAttribute("branch", branch);
	        model.addAttribute("users", userRepository.findAll());

	        model.addAttribute("pageTitle", "Chỉnh Sửa");
	        model.addAttribute("parentPageTitle", "Quản lý Cửa hàng");
	        model.addAttribute("parentPageUrl", "/admin/branches");

	        model.addAttribute("currentPage", "branches");
	        return "admin/branch-form";
	    }

	    @PostMapping("/branches/save")
	    public String saveBranch(@Valid @ModelAttribute("branch") Branch branch,
	                             BindingResult bindingResult,
	                             RedirectAttributes redirectAttributes,
	                             Model model) {
	        if (bindingResult.hasErrors()) {
	            model.addAttribute("users", userRepository.findAll());
	            model.addAttribute("pageTitle", branch.getId() == null ? "Thêm Mới" : "Chỉnh Sửa");
	            model.addAttribute("parentPageTitle", "Quản lý Cửa hàng");
	            model.addAttribute("parentPageUrl", "/admin/branches");
	            return "admin/branch-form";
	        }

	        try {
	            branchService.save(branch);
	            redirectAttributes.addFlashAttribute("message", "Lưu chi nhánh thành công!");
	        } catch (DataIntegrityViolationException e) {
	            // Bắt lỗi trùng slug và báo lỗi ngay tại trường Tên
	            bindingResult.rejectValue("name", "error.branch", "Tên chi nhánh này đã tồn tại (tạo ra slug bị trùng).");
	            model.addAttribute("users", userRepository.findAll());
	            model.addAttribute("pageTitle", branch.getId() == null ? "Thêm Mới" : "Chỉnh Sửa");
	            model.addAttribute("parentPageTitle", "Quản lý Cửa hàng");
	            model.addAttribute("parentPageUrl", "/admin/branches");
	            return "admin/branch-form";
	        }

	        return "redirect:/admin/branches";
	    }

	    @GetMapping("/branches/delete/{id}")
	    public String deleteBranch(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
	         try {
	            branchService.deleteById(id);
	            redirectAttributes.addFlashAttribute("message", "Xóa chi nhánh thành công!");
	        } catch (DataIntegrityViolationException e) {
	            redirectAttributes.addFlashAttribute("error", "Lỗi: Không thể xóa chi nhánh này vì đã có dữ liệu liên quan.");
	        } catch (Exception e) {
	            redirectAttributes.addFlashAttribute("error", "Lỗi: Không tìm thấy chi nhánh để xóa.");
	        }
	        return "redirect:/admin/branches";
	    }
	 // Tiêm CategoryService


	    // --- QUẢN LÝ DANH MỤC ---

	    @GetMapping("/categories")
	    public String showCategoryPage(Model model) {
	        model.addAttribute("categoryList", categoryService.findAll());
	        model.addAttribute("pageTitle", "Quản lý Danh mục");
	        model.addAttribute("currentPage", "categories");
	        return "admin/categories";
	    }

	    @GetMapping("/categories/add")
	    public String showAddCategoryForm(Model model) {
	        model.addAttribute("category", new Category());
	        model.addAttribute("categoryList", categoryService.findAll()); // Để chọn danh mục cha
	        model.addAttribute("pageTitle", "Thêm Danh mục mới");
	        model.addAttribute("parentPageTitle", "Quản lý Danh mục");
	        model.addAttribute("parentPageUrl", "/admin/categories");
	        model.addAttribute("currentPage", "categories");
	        return "admin/category-form";
	    }

	    @GetMapping("/categories/edit/{id}")
	    public String showEditCategoryForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
	        Category category = categoryService.findById(id);
	        if (category == null) {
	            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục!");
	            return "redirect:/admin/categories";
	        }
	        model.addAttribute("category", category);
	        model.addAttribute("categoryList", categoryService.findAll());
	        model.addAttribute("pageTitle", "Sửa Danh mục");
	        model.addAttribute("parentPageTitle", "Quản lý Danh mục");
	        model.addAttribute("parentPageUrl", "/admin/categories");
	        model.addAttribute("currentPage", "categories");
	        return "admin/category-form";
	    }

	    @PostMapping("/categories/save")
	    public String saveCategory(@Valid @ModelAttribute("category") Category category, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
	        if (bindingResult.hasErrors()) {
	            model.addAttribute("categoryList", categoryService.findAll());
	            return "admin/category-form";
	        }
	        try {
	            categoryService.save(category);
	            redirectAttributes.addFlashAttribute("message", "Lưu danh mục thành công!");
	        } catch (DataIntegrityViolationException e) {
	            redirectAttributes.addFlashAttribute("error", "Lỗi: Slug đã tồn tại!");
	        }
	        return "redirect:/admin/categories";
	    }

	    @GetMapping("/categories/delete/{id}")
	    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
	        try {
	            categoryService.deleteById(id);
	            redirectAttributes.addFlashAttribute("message", "Xóa danh mục thành công!");
	        } catch (Exception e) {
	            redirectAttributes.addFlashAttribute("error", "Không thể xóa danh mục này (có thể do đang chứa sản phẩm hoặc danh mục con).");
	        }
	        return "redirect:/admin/categories";
	    }



	    // --- QUẢN LÝ TOPPING ---

	    @GetMapping("/toppings")
	    public String showToppingPage(Model model) {
	        model.addAttribute("toppingList", toppingService.findAll());
	        model.addAttribute("pageTitle", "Quản lý Topping");
	        model.addAttribute("currentPage", "toppings");
	        return "admin/toppings";
	    }

	    @GetMapping("/toppings/add")
	    public String showAddToppingForm(Model model) {
	        model.addAttribute("topping", new Topping());
	        model.addAttribute("pageTitle", "Thêm Topping mới");
	        model.addAttribute("parentPageTitle", "Quản lý Topping");
	        model.addAttribute("parentPageUrl", "/admin/toppings");
	        model.addAttribute("currentPage", "toppings");
	        return "admin/topping-form";
	    }

	    @GetMapping("/toppings/edit/{id}")
	    public String showEditToppingForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
	        Topping topping = toppingService.findById(id);
	        if (topping == null) {
	            redirectAttributes.addFlashAttribute("error", "Không tìm thấy topping!");
	            return "redirect:/admin/toppings";
	        }
	        model.addAttribute("topping", topping);
	        model.addAttribute("pageTitle", "Sửa Topping");
	        model.addAttribute("parentPageTitle", "Quản lý Topping");
	        model.addAttribute("parentPageUrl", "/admin/toppings");
	        model.addAttribute("currentPage", "toppings");
	        return "admin/topping-form";
	    }

	    @PostMapping("/toppings/save")
	    public String saveTopping(@Valid @ModelAttribute("topping") Topping topping,
	                              BindingResult bindingResult,
	                              RedirectAttributes redirectAttributes,
	                              Model model) { // Thêm Model vào tham số
	        if (bindingResult.hasErrors()) {
	            // Thêm các model attribute cần thiết nếu có lỗi validation
	            model.addAttribute("pageTitle", topping.getId() == null ? "Thêm Mới" : "Sửa");
	            model.addAttribute("parentPageTitle", "Quản lý Topping");
	            model.addAttribute("parentPageUrl", "/admin/toppings");
	            return "admin/topping-form";
	        }

	        try {
	            toppingService.save(topping);
	            redirectAttributes.addFlashAttribute("message", "Lưu topping thành công!");
	        } catch (DataIntegrityViolationException e) {
	            // Bắt lỗi trùng lặp và gán lỗi vào trường 'name'
	            bindingResult.rejectValue("name", "error.topping", "Tên topping này đã tồn tại.");

	            // Chuẩn bị dữ liệu để hiển thị lại form
	            model.addAttribute("pageTitle", topping.getId() == null ? "Thêm Mới" : "Sửa");
	            model.addAttribute("parentPageTitle", "Quản lý Topping");
	            model.addAttribute("parentPageUrl", "/admin/toppings");
	            return "admin/topping-form";
	        }

	        return "redirect:/admin/toppings";
	    }

	    @GetMapping("/toppings/delete/{id}")
	    public String deleteTopping(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
	        try {
	            toppingService.deleteById(id);
	            redirectAttributes.addFlashAttribute("message", "Xóa topping thành công!");
	        } catch (Exception e) {
	            redirectAttributes.addFlashAttribute("error", "Không thể xóa topping này.");
	        }
	        return "redirect:/admin/toppings";
	    }

	 // --- QUẢN LÝ KÍCH THƯỚC (SIZE) ---

	    @GetMapping("/sizes")
	    public String showSizes(Model model) {
	        List<Size> sizeList = sizeService.findAll();
	        model.addAttribute("sizeList", sizeList);
	        model.addAttribute("pageTitle", "Quản lý Kích thước");
	        model.addAttribute("currentPage", "sizes");

	        return "admin/sizes"; // ✅ Thymeleaf tự load layout qua th:replace
	    }


	    @GetMapping("/products")
    public String showProductPage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Sản phẩm");
        model.addAttribute("currentPage", "products");
        return "admin/products"; // Trỏ đến file products.html
    }

    @GetMapping("/orders")
    public String showOrderPage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Đơn hàng");
        model.addAttribute("currentPage", "orders");
        return "admin/orders"; // Trỏ đến file orders.html
    }

    @GetMapping("/marketing")
    public String showMarketingPage(Model model) {
        model.addAttribute("pageTitle", "Marketing & Khuyến mãi");
        model.addAttribute("currentPage", "marketing");
        return "admin/marketing"; // Trỏ đến file marketing.html
    }

    @GetMapping("/reports")
    public String showReportPage(Model model) {
        model.addAttribute("pageTitle", "Báo cáo & Thống kê");
        model.addAttribute("currentPage", "reports");
        return "admin/reports"; // Trỏ đến file reports.html
    }

    @GetMapping("/shipping")
    public String showShippingPage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Vận chuyển");
        model.addAttribute("currentPage", "shipping");
        return "admin/shipping"; // Trỏ đến file shipping.html
    }

 // --- QUẢN LÝ NHÀ VẬN CHUYỂN ---
    @GetMapping("/shipping-carriers")
    public String showShippingCarriersPage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Nhà vận chuyển");
        model.addAttribute("currentPage", "shipping-carriers");
        return "admin/shipping_carriers"; // Trỏ tới file shipping_carriers.html
    }


    @GetMapping("/settings")
    public String showSettingPage(Model model) {
        model.addAttribute("pageTitle", "Cài đặt");
        model.addAttribute("currentPage", "settings");
        return "admin/settings"; // Trỏ đến file settings.html
    }

    // ======================= 📢 KHUYẾN MÃI (PROMOTIONS) =======================

    @GetMapping("/promotions/campaign-list")
    public String showCampaignList(Model model) {
        model.addAttribute("pageTitle", "Chiến dịch khuyến mãi");
        model.addAttribute("currentPage", "campaigns");
        return "admin/promotions/campaign-list";
    }

    @GetMapping("/promotions/coupon-list")
    public String showCouponList(Model model) {
        model.addAttribute("pageTitle", "Mã giảm giá");
        model.addAttribute("currentPage", "coupons");
        return "admin/promotions/coupon-list";
    }

    @GetMapping("/promotions/campaign-target")
    public String showCampaignTarget(Model model) {
        model.addAttribute("pageTitle", "Đối tượng áp dụng");
        model.addAttribute("currentPage", "targets");
        return "admin/promotions/campaign-target";
    }

    @GetMapping("/chat")
    public String showChatDashboard(Model model) {
        model.addAttribute("pageTitle", "Quản lý Chat");
        model.addAttribute("currentPage", "chat");
        return "admin/admin-chat-dashboard";
    }

}
