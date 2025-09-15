// 📁 com/alotra/repository/ProductRepository.java
package com.alotra.repository;

import com.alotra.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}