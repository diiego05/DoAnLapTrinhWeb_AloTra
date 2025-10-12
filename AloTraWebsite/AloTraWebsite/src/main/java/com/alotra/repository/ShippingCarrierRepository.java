package com.alotra.repository;

import com.alotra.entity.ShippingCarrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingCarrierRepository extends JpaRepository<ShippingCarrier, Long> {
    List<ShippingCarrier> findAllByOrderByIdDesc();
    List<ShippingCarrier> findByIsActiveTrue();
}
