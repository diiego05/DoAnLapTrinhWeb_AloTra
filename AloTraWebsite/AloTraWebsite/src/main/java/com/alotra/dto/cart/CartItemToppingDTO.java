package com.alotra.dto.cart;

import java.math.BigDecimal;

public class CartItemToppingDTO {
    private Long id;
    private Long toppingId;
    private String name;
    private BigDecimal price;

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getToppingId(){return toppingId;}
    public void setToppingId(Long toppingId){this.toppingId=toppingId;}
    public String getName(){return name;}
    public void setName(String name){this.name=name;}
    public BigDecimal getPrice(){return price;}
    public void setPrice(BigDecimal price){this.price=price;}
}
