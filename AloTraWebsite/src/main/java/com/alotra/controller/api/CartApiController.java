package com.alotra.controller.api;

import com.alotra.dto.ProductVariantDTO;
import com.alotra.dto.cart.AddCartItemRequest;
import com.alotra.dto.cart.UpdateCartItemRequest;
import com.alotra.dto.cart.CartResponse;
import com.alotra.service.CartService;
import com.alotra.service.UserService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {
    private final CartService cartService;
    private final UserService userService;

    public CartApiController(CartService cartService,UserService userService){
        this.cartService=cartService;
        this.userService=userService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(){
        Long uid=userService.getCurrentUserId();
        return ResponseEntity.ok(cartService.getCart(uid));
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItem(@RequestBody AddCartItemRequest req){
        Long uid=userService.getCurrentUserId();
        cartService.addItem(uid,req);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<Void> updateItem(@PathVariable Long id,@RequestBody UpdateCartItemRequest req){
        Long uid=userService.getCurrentUserId();
        cartService.updateItem(uid,id,req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable Long id){
        Long uid=userService.getCurrentUserId();
        cartService.removeItem(uid,id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> clear(){
        Long uid=userService.getCurrentUserId();
        cartService.clear(uid);
        return ResponseEntity.ok().build();
    }

}
