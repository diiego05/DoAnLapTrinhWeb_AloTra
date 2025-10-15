package com.alotra.dto.cart;

import java.util.List;

public class AddCartItemRequest {
    private Long productId;
    private Long variantId;
    private Integer quantity;
    private List<Long> toppingIds;
    private String note;

    public Long getProductId(){return productId;}
    public void setProductId(Long productId){this.productId=productId;}
    public Long getVariantId(){return variantId;}
    public void setVariantId(Long variantId){this.variantId=variantId;}
    public Integer getQuantity(){return quantity;}
    public void setQuantity(Integer quantity){this.quantity=quantity;}
    public List<Long> getToppingIds(){return toppingIds;}
    public void setToppingIds(List<Long> toppingIds){this.toppingIds=toppingIds;}
    public String getNote(){return note;}
    public void setNote(String note){this.note=note;}
}
