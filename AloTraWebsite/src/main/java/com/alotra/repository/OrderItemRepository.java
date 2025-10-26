package com.alotra.repository;

import com.alotra.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 📌 Lấy toàn bộ sản phẩm thuộc một đơn hàng
    List<OrderItem> findByOrderId(Long orderId);

    // 📌 Tuỳ chọn: nếu bạn muốn xoá tất cả item khi huỷ đơn hàng
    void deleteByOrderId(Long orderId);
}
