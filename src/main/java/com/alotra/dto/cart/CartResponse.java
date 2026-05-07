package com.alotra.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {
    private List<CartItemResponse> items;
    private Integer itemsCount;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal total;

    public List<CartItemResponse> getItems(){return items;}
    public void setItems(List<CartItemResponse> items){this.items=items;}
    public Integer getItemsCount(){return itemsCount;}
    public void setItemsCount(Integer itemsCount){this.itemsCount=itemsCount;}
    public BigDecimal getSubtotal(){return subtotal;}
    public void setSubtotal(BigDecimal subtotal){this.subtotal=subtotal;}
    public BigDecimal getShippingFee(){return shippingFee;}
    public void setShippingFee(BigDecimal shippingFee){this.shippingFee=shippingFee;}
    public BigDecimal getTotal(){return total;}
    public void setTotal(BigDecimal total){this.total=total;}
}
