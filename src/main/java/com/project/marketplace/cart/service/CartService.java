package com.project.marketplace.cart.service;

import com.project.marketplace.cart.dto.CartItemRequestDto;
import com.project.marketplace.cart.dto.CartResponseDto;
import com.project.marketplace.cart.entity.Cart;
import com.project.marketplace.cart.entity.CartItem;
import com.project.marketplace.cart.repository.CartRepository;
import com.project.marketplace.product.entity.Product;
import com.project.marketplace.product.repository.ProductRepository;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // 사용자 기준 장바구니를 조회하되 없으면 빈 응답을 내려 프론트 흐름이 끊기지 않게 했다.
    @Transactional(readOnly = true)
    public CartResponseDto getCartByUserId(Long userId) {
        return cartRepository.findDetailByUserId(userId)
                .map(CartResponseDto::fromEntity)
                .orElseGet(() -> CartResponseDto.empty(userId));
    }

    // 장바구니 항목 추가를 서비스에서 일괄 처리해 중복 상품 병합과 스냅샷 저장 규칙을 통일한다.
    @Transactional
    public CartResponseDto addItem(Long userId, CartItemRequestDto requestDto) {
        validateAddRequest(requestDto);
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. ID: " + requestDto.getProductId()));

        Cart cart = getOrCreateCart(userId);
        cart.getCartItems().stream()
                .filter(cartItem -> cartItem.getProduct().getId().equals(product.getId()))
                .findFirst()
                .ifPresentOrElse(
                        // 같은 상품이 이미 있으면 수량만 합산해 장바구니 라인 중복을 막는다.
                        cartItem -> cartItem.changeQuantity(cartItem.getQuantity() + requestDto.getQuantity()),
                        // 없는 상품이면 새 CartItem을 만들어 Cart에 연결한다.
                        () -> cart.addCartItem(CartItem.create(product, requestDto.getQuantity()))
                );

        return CartResponseDto.fromEntity(cartRepository.save(cart));
    }

    // 항목 수량 변경 시 Cart 소유자/항목 소속 검증을 함께 수행해 잘못된 갱신을 막는다.
    @Transactional
    public CartResponseDto updateItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("수량은 1 이상이어야 합니다.");
        }

        Cart cart = getExistingCart(userId);
        CartItem cartItem = findCartItem(cart, cartItemId);
        cartItem.changeQuantity(quantity);
        return CartResponseDto.fromEntity(cartRepository.save(cart));
    }

    // 특정 항목만 제거하는 API를 제공해 장바구니 전체 재구성 없이 부분 삭제가 가능하게 했다.
    @Transactional
    public CartResponseDto removeItem(Long userId, Long cartItemId) {
        Cart cart = getExistingCart(userId);
        CartItem cartItem = findCartItem(cart, cartItemId);
        cart.removeCartItem(cartItem);
        return CartResponseDto.fromEntity(cartRepository.save(cart));
    }

    // 체크아웃 취소 같은 케이스를 위해 사용자 장바구니 전체 비우기 기능을 추가했다.
    @Transactional
    public CartResponseDto clearCart(Long userId) {
        Cart cart = getExistingCart(userId);
        // 순회 중 컬렉션 수정 예외를 피하려고 복사본을 기준으로 항목을 제거한다.
        List.copyOf(cart.getCartItems()).forEach(cart::removeCartItem);
        return CartResponseDto.fromEntity(cartRepository.save(cart));
    }

    // 항목 추가 시에는 장바구니가 없을 수 있어 사용자 검증 후 신규 장바구니를 자동 생성한다.
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findDetailByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    // 수량 수정/삭제는 기존 장바구니가 필요하므로 사용자 장바구니 미존재를 명확히 예외 처리한다.
    private Cart getExistingCart(Long userId) {
        return cartRepository.findDetailByUserId(userId)
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다. userId: " + userId));
    }

    // Cart 내부에서 항목 ID를 탐색해 다른 사용자 장바구니 항목 접근을 차단한다.
    private CartItem findCartItem(Cart cart, Long cartItemId) {
        return cart.getCartItems().stream()
                .filter(cartItem -> cartItem.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("장바구니 항목을 찾을 수 없습니다. cartItemId: " + cartItemId));
    }

    // 항목 추가 입력값을 서비스 초입에서 검증해 null/음수 데이터 저장을 방지한다.
    private void validateAddRequest(CartItemRequestDto requestDto) {
        if (requestDto == null || requestDto.getProductId() == null) {
            throw new RuntimeException("상품 ID는 필수입니다.");
        }
        if (requestDto.getQuantity() == null || requestDto.getQuantity() <= 0) {
            throw new RuntimeException("수량은 1 이상이어야 합니다.");
        }
    }
}
