package com.alotra.service;

import com.alotra.entity.ShippingCarrier;
import com.alotra.repository.ShippingCarrierRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShippingCarrierService {

    private final ShippingCarrierRepository repo;

    public ShippingCarrierService(ShippingCarrierRepository repo) {
        this.repo = repo;
    }

    public List<ShippingCarrier> getAll() {
        return repo.findAllByOrderByIdDesc();
    }

    public ShippingCarrier create(ShippingCarrier carrier) {
        return repo.save(carrier);
    }

    public ShippingCarrier update(Long id, ShippingCarrier updated) {
        ShippingCarrier c = repo.findById(id).orElseThrow();
        c.setName(updated.getName());
        c.setLogoUrl(updated.getLogoUrl());
        c.setBaseFee(updated.getBaseFee());
        c.setActive(updated.isActive());
        return repo.save(c);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void toggleStatus(Long id) {
        ShippingCarrier c = repo.findById(id).orElseThrow();
        c.setActive(!c.isActive());
        repo.save(c);
    }

    public List<ShippingCarrier> getActiveCarriers() {
        return repo.findByIsActiveTrue();
    }
}
