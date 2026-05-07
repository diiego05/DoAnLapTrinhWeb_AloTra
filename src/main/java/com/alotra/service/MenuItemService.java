package com.alotra.service;

import com.alotra.dto.MenuItemDTO;
import com.alotra.entity.Category;
import com.alotra.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final CategoryRepository categoryRepository;

    /**
     * Lấy menu cho header từ Categories (cache 1 giờ)
     */
    @Cacheable(value = "menuCache", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<MenuItemDTO> getPublicMenu() {
        List<Category> topLevelCategories = categoryRepository.findTopLevelCategories();
        return topLevelCategories.stream()
                .map(MenuItemDTO::from)
                .collect(Collectors.toList());
    }
}