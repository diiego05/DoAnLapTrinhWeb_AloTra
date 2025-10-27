package com.alotra.controller.api;

import com.alotra.service.CategoryMenuService;
import com.alotra.service.CategoryMenuService.CategoryMenuDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryMenuController {

    private final CategoryMenuService categoryMenuService;

    /**
     * 🗺️ API lấy menu động từ Categories
     * Endpoint: GET /api/categories/menu
     * Return: Danh sách categories với cấu trúc parent-child dùng cho menu
     */
    @GetMapping("/menu")
    public ResponseEntity<List<CategoryMenuDTO>> getMenu() {
        List<CategoryMenuDTO> menuItems = categoryMenuService.buildMenu();
        return ResponseEntity.ok(menuItems);
    }

    /**
     * 🌲 API lấy cây danh mục (alias cho /menu)
     * Endpoint: GET /api/categories/tree
     * Return: Danh sách categories với cấu trúc parent-child
     */
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryMenuDTO>> getTree() {
        List<CategoryMenuDTO> menuItems = categoryMenuService.buildMenu();
        return ResponseEntity.ok(menuItems);
    }
}