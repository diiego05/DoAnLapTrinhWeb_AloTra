package com.alotra.controller.api;

import com.alotra.entity.Topping;
import com.alotra.repository.ToppingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/toppings")
public class ToppingApiController {

    private final ToppingRepository toppingRepository;

    public ToppingApiController(ToppingRepository toppingRepository) {
        this.toppingRepository = toppingRepository;
    }

    // 🟢 Lấy danh sách topping đang hoạt động
    @GetMapping
    public List<Topping> getAllActiveToppings() {
        return toppingRepository.findByStatus("ACTIVE");
    }

    // 🟡 (Tuỳ chọn) Thêm topping mới (admin)
    @PostMapping
    public Topping create(@RequestBody Topping topping) {
        topping.setStatus("ACTIVE");
        return toppingRepository.save(topping);
    }

    // 🔴 (Tuỳ chọn) Xoá topping
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        toppingRepository.deleteById(id);
    }
}
