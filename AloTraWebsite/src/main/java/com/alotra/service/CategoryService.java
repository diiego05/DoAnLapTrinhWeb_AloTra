package com.alotra.service;

import com.alotra.entity.Category;
import com.alotra.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private String generateSlug(String input) {
        if (input == null) {
            return "";
        }
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public void save(Category category) {
        // ✅ Tự động tạo slug
        category.setSlug(generateSlug(category.getName()));

        // ✅ Xử lý parent để tránh detached entity
        if (category.getParent() != null && category.getParent().getId() != null) {
            Category parent = categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục cha"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        categoryRepository.save(category);
    }

    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
    }
}
