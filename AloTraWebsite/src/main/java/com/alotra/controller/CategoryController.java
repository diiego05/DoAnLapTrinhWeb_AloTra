package com.alotra.controller;
import com.alotra.entity.Category;
import com.alotra.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
@Controller
public class CategoryController {
	@Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/categories") // Xử lý yêu cầu tại http://.../alotra-website/categories
    public String showCategoryList(Model model) {
        List<Category> allCategories = categoryRepository.findAll();
        model.addAttribute("categories", allCategories);
        model.addAttribute("pageTitle", "Tất Cả Danh Mục");
        return "category/list"; // Trả về file list.html trong templates/category
    }
}
