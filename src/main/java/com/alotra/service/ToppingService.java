package com.alotra.service;

import com.alotra.entity.Topping;
import com.alotra.repository.ToppingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToppingService {

    @Autowired
    private ToppingRepository toppingRepository;

    public List<Topping> findAll() {
        return toppingRepository.findAll();
    }

    public Topping findById(Long id) {
        return toppingRepository.findById(id).orElse(null);
    }

    public void save(Topping topping) {
        toppingRepository.save(topping);
    }

    public void deleteById(Long id) {
        toppingRepository.deleteById(id);
    }
}