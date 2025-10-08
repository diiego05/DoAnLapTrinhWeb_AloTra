package com.alotra.controller;

import com.alotra.entity.CartItem;
import com.alotra.entity.ProductVariant;
import com.alotra.repository.ProductVariantRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.*;

@Controller
@RequestMapping("/cart")
@SessionAttributes("cart")
public class CartController {

    private final ProductVariantRepository variantRepo;

    public CartController(ProductVariantRepository variantRepo) {
        this.variantRepo = variantRepo;
    }

    @ModelAttribute("cart")
    public Map<Long, CartItem> initCart() {
        return new HashMap<>();
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam("variantId") Long variantId,
                            @RequestParam("quantity") int quantity,
                            @ModelAttribute("cart") Map<Long, CartItem> cart) {

        ProductVariant variant = variantRepo.findById(variantId).orElse(null);
        if (variant == null) return "redirect:/";

        CartItem item = cart.get(variantId);
        if (item == null) {
            item = new CartItem(variant, quantity);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }
        cart.put(variantId, item);

        return "redirect:/cart/view";
    }

    @GetMapping("/view")
    public String viewCart(@ModelAttribute("cart") Map<Long, CartItem> cart, Model model) {
        model.addAttribute("cartItems", cart.values());
        model.addAttribute("total", cart.values().stream()
                .map(i -> i.getVariant().getPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        return "cart";
    }
}
