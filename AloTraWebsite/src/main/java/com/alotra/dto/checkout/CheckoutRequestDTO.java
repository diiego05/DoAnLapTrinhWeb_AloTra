// com/alotra/dto/checkout/CheckoutRequestDTO.java
package com.alotra.dto.checkout;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequestDTO {
    private List<Long> cartItemIds;   // danh sách item được chọn từ giỏ
    private Long addressId;           // địa chỉ nhận
    private Long branchId;            // chi nhánh thực hiện (có thể null nếu PICKUP chưa cần)
    private Long shippingCarrierId;   // đơn vị VC (null nếu PICKUP)
    private String couponCode;        // optional
    private String paymentMethod;     // PICKUP | COD | BANK_TRANSFER
    private String note;              // ghi chú chung đơn hàng
}
