package com.project.marketplace.cart.controller;


import com.project.marketplace.cart.dto.CartItemQuantityUpdateDto;
import com.project.marketplace.cart.dto.CartItemRequestDto;
import com.project.marketplace.cart.dto.CartResponseDto;
import com.project.marketplace.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;


    @GetMapping("/{userId}")
    public ResponseEntity<CartResponseDto> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }


    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponseDto> addItem(
            @PathVariable Long userId,
            @RequestBody CartItemRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(userId, requestDto));
    }

    @PatchMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponseDto> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestBody CartItemQuantityUpdateDto requestDto) {
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, cartItemId, requestDto.getQuantity()));
    }


    @DeleteMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponseDto> removeItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(cartService.removeItem(userId, cartItemId));
    }


    @DeleteMapping("/{userId}/items")
    public ResponseEntity<CartResponseDto> clearCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.clearCart(userId));
    }
}
