// com/alotra/dto/checkout/CheckoutItemRequest.java
package com.alotra.dto.checkout;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutItemRequest {
    private Long cartItemId;        // id trong CartItems
    private List<Long> toppingIds;  // nếu FE muốn “ép” lại topping tại màn checkout
}
