package com.alotra.controller.api;

import com.alotra.entity.Size;
import com.alotra.service.SizeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sizes")
public class SizeApiController {

    @Autowired
    private SizeService sizeService;

    // API: Lấy tất cả sizes
    @GetMapping
    public ResponseEntity<List<Size>> getAll() {
        return ResponseEntity.ok(sizeService.findAll());
    }

    // API: Lấy 1 size theo ID
    @GetMapping("/{id}")
    public ResponseEntity<Size> getById(@PathVariable Long id) {
        Size size = sizeService.findById(id);
        if (size != null) {
            return ResponseEntity.ok(size);
        }
        return ResponseEntity.notFound().build();
    }

    // API: Tạo mới 1 size
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Size size, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }
        try {
            sizeService.save(size);
            return new ResponseEntity<>(size, HttpStatus.CREATED);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // API: Cập nhật 1 size
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Size sizeDetails, BindingResult bindingResult) {
        if (sizeService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }
        try {
            sizeDetails.setId(id); // Đảm bảo đúng ID
            sizeService.save(sizeDetails);
            return ResponseEntity.ok(sizeDetails);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // API: Xóa 1 size
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (sizeService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        sizeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}