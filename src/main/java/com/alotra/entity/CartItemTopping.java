package com.alotra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="CartItemToppings",uniqueConstraints=@UniqueConstraint(columnNames={"CartItemId","ToppingId"}))
public class CartItemTopping {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="Id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="CartItemId",nullable=false)
    private CartItem cartItem;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ToppingId",nullable=false)
    private Topping topping;

    @Column(name="PriceAtAddition",nullable=false,precision=12,scale=2)
    private BigDecimal priceAtAddition;

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public CartItem getCartItem(){return cartItem;}
    public void setCartItem(CartItem cartItem){this.cartItem=cartItem;}
    public Topping getTopping(){return topping;}
    public void setTopping(Topping topping){this.topping=topping;}
    public BigDecimal getPriceAtAddition(){return priceAtAddition;}
    public void setPriceAtAddition(BigDecimal priceAtAddition){this.priceAtAddition=priceAtAddition;}
}
