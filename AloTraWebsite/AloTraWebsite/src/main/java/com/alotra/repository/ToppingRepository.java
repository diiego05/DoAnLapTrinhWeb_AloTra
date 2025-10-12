package com.alotra.repository;

import com.alotra.entity.Topping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, Long> {
    // Chỉ lấy các topping đang ACTIVE
    List<Topping> findByStatus(String status);
}
