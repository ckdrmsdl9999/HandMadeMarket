package com.project.marketplace.cart.controller;


import com.project.marketplace.cart.dto.CartItemQuantityUpdateDto;
import com.project.marketplace.cart.dto.CartItemRequestDto;
import com.project.marketplace.cart.dto.CartResponseDto;
import com.project.marketplace.cart.service.CartService;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {


    private final CartService cartService;
    private final UserService userService;


    @GetMapping("/{userId}")
    public ResponseEntity<CartResponseDto> getCart(@PathVariable Long userId, Authentication authentication) {
        // 장바구니 조회를 로그인 사용자 본인에게만 허용해 임의 userId 접근을 막는다.
        validateCartAccess(userId, authentication);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }


    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponseDto> addItem(
            @PathVariable Long userId,
            Authentication authentication,
            @RequestBody CartItemRequestDto requestDto) {
        validateCartAccess(userId, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(userId, requestDto));
    }


    @PatchMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponseDto> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            Authentication authentication,
            @RequestBody CartItemQuantityUpdateDto requestDto) {
        validateCartAccess(userId, authentication);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, cartItemId, requestDto.getQuantity()));
    }


    @DeleteMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponseDto> removeItem(
            @PathVariable Long userId,
            Authentication authentication,
            @PathVariable Long cartItemId) {
        validateCartAccess(userId, authentication);
        return ResponseEntity.ok(cartService.removeItem(userId, cartItemId));
    }

    @DeleteMapping("/{userId}/items")
    public ResponseEntity<CartResponseDto> clearCart(@PathVariable Long userId, Authentication authentication) {
        validateCartAccess(userId, authentication);
        return ResponseEntity.ok(cartService.clearCart(userId));
    }


    private void validateCartAccess(Long requestedUserId, Authentication authentication) {
        Long authenticatedUserId = resolveAuthenticatedUserId(authentication);
        if (authenticatedUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 후 장바구니를 이용할 수 있습니다.");
        }
        if (!authenticatedUserId.equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 사용자의 장바구니에는 접근할 수 없습니다.");
        }
    }


    private Long resolveAuthenticatedUserId(Authentication authentication) {
        // 현재 인증 사용자 해석을 UserService로 통일해 로컬/OAuth2 분기 중복 제거 -5/29
        return userService.getAuthenticatedUser(authentication)
                .map(User::getId)
                .orElse(null);
    }




}



