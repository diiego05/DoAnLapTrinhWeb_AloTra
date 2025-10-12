package com.alotra.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name="Carts")
public class Cart {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="Id")
    private Long id;

    @Column(name="UserId",nullable=false)
    private Long userId;

    @OneToMany(mappedBy="cart",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY)
    private List<CartItem> items=new ArrayList<>();

    public Cart(){}
    public Cart(Long userId){this.userId=userId;}
    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getUserId(){return userId;}
    public void setUserId(Long userId){this.userId=userId;}
    public List<CartItem> getItems(){return items;}
    public void setItems(List<CartItem> items){this.items=items;}
}
