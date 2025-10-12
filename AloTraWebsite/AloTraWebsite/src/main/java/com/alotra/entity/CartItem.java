package com.alotra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name="CartItems",uniqueConstraints=@UniqueConstraint(columnNames={"CartId","VariantId"}))
public class CartItem {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="Id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="CartId",nullable=false)
    private Cart cart;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="VariantId",nullable=false)
    private ProductVariant variant;

    @Column(name="Quantity",nullable=false)
    private Integer quantity;

    @Column(name="PriceAtAddition",nullable=false,precision=12,scale=2)
    private BigDecimal priceAtAddition;

    @Column(name="Note")
    private String note;

    @OneToMany(mappedBy="cartItem",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY)
    private List<CartItemTopping> toppings=new ArrayList<>();

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Cart getCart(){return cart;}
    public void setCart(Cart cart){this.cart=cart;}
    public ProductVariant getVariant(){return variant;}
    public void setVariant(ProductVariant variant){this.variant=variant;}
    public Integer getQuantity(){return quantity;}
    public void setQuantity(Integer quantity){this.quantity=quantity;}
    public BigDecimal getPriceAtAddition(){return priceAtAddition;}
    public void setPriceAtAddition(BigDecimal priceAtAddition){this.priceAtAddition=priceAtAddition;}
    public String getNote(){return note;}
    public void setNote(String note){this.note=note;}
    public List<CartItemTopping> getToppings(){return toppings;}
    public void setToppings(List<CartItemTopping> toppings){this.toppings=toppings;}
}
