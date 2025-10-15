// 📁 com/alotra/controller/ProductController.java
package com.alotra.controller;

import com.alotra.entity.Product;
import com.alotra.entity.ProductVariant; // ✅ sửa lại đúng tên entity
import com.alotra.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model) {
        Product product = productService.findById(id);
        if (product == null) {
            return "redirect:/";
        }

        // ✅ Sắp xếp các biến thể theo giá tăng dần
        List<ProductVariant> sortedVariants = product.getVariants()
                .stream()
                .sorted(Comparator.comparing(ProductVariant::getPrice))
                .collect(Collectors.toList());

        model.addAttribute("product", product);
        model.addAttribute("sortedVariants", sortedVariants); // Gửi danh sách đã sắp xếp
        model.addAttribute("pageTitle", product.getName());
        return "products/product_detail";
    }
}
