package com.project.marketplace.product.controller;

import com.project.marketplace.product.dto.ProductDto;
import com.project.marketplace.product.service.ProductService;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    // 상품 등록 시 프론트 sellerId 대신 로그인 사용자 ID를 서버에서 결정하게 사용자 저장소를 추가함 -3/18
    private final UserRepository userRepository;

    /**
     * 모든 상품 목록을 조회합니다.
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // 판매자 페이지에서 로그인한 사용자 상품만 분리 조회하게 내 상품 API를 추가함 -3/19
    @GetMapping("/mine")
    public ResponseEntity<List<ProductDto>> getMyProducts(Authentication authentication) {
        Long currentUserId = resolveCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ProductDto> products = productService.getProductsBySellerId(currentUserId);
        return ResponseEntity.ok(products);
    }

    /**
     * 상품 상세 정보를 조회합니다.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long productId) {
        ProductDto product = productService.getProductById(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    /**
     * 카테고리별 상품 목록을 조회합니다.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(@PathVariable String category) {
        List<ProductDto> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }

    /**
     * 상품명으로 상품을 검색합니다.
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> searchProducts(@RequestParam String keyword) {
        List<ProductDto> products = productService.searchProductsByName(keyword);
        return ResponseEntity.ok(products);
    }

    /**
     * 새로운 상품을 등록합니다.
     */
    @PostMapping
    public ResponseEntity<Map<String, Long>> createProduct(@RequestBody ProductDto productDto, Authentication authentication) {
        // 상품 등록 판매자를  현재 로그인 사용자 기준으로정함
        Long currentUserId = resolveCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        productDto.setSellerId(currentUserId);
        Long productId = productService.createProduct(productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("productId", productId));
    }

    /**
     * 상품 정보를 수정합니다.
     */
    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductDto productDto) {
        // 컨트롤러의 사전 조회를 제거하고 경로 ID와 본문 ID 일치만 검증해 서비스 단 조회로 중복 쿼리를 줄였다.
        if (!productId.equals(productDto.getProductId())) {
            return ResponseEntity.badRequest().build();
        }
        productService.updateProduct(productDto);

        return ResponseEntity.ok().build();
    }

    /**
     * 상품을 삭제합니다.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId, Authentication authentication) {
        // 상품 삭제도 등록 권한 검사
        Long currentUserId = resolveCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 삭제 요청은 서비스에서 판매자 본인 여부까지 확인하도록함
        productService.deleteProduct(productId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 상품 재고를 업데이트합니다.
     */
    @PatchMapping("/{productId}/quantity")
    public ResponseEntity<Void> updateProductQuantity(
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> request) {

        try {
            Integer quantity = request.get("quantity");
            if (quantity == null) {
                return ResponseEntity.badRequest().build();
            }

            productService.updateProductQuantity(productId, quantity);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 인기 상품 목록을 조회합니다.
     */
    @GetMapping("/popular")
    public ResponseEntity<List<ProductDto>> getPopularProducts(
            @RequestParam(defaultValue = "10") int limit) {

        List<ProductDto> products = productService.getPopularProducts(limit);
        return ResponseEntity.ok(products);
    }

    /**
     * 상품 구매 처리를 수행합니다.
     */
    @PostMapping("/{productId}/purchase")
    public ResponseEntity<Void> purchaseProduct(
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> request) {

        try {
            Integer quantity = request.get("quantity");
            if (quantity == null || quantity <= 0) {
                return ResponseEntity.badRequest().build();
            }

            productService.purchaseProduct(productId, quantity);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 상품 등록 API도 홈/장바구니와 같은 기준으로 현재 로그인 사용자를 찾아 sellerId로 재사용하게 맞춤 -3/18
    private Long resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            Object responseObj = oauthToken.getPrincipal().getAttributes().get("response");
            if (responseObj instanceof Map<?, ?> response) {
                Object providerId = response.get("id");
                if (providerId instanceof String providerIdText && !providerIdText.isBlank()) {
                    return userRepository.findByProviderAndLoginId(
                                    oauthToken.getAuthorizedClientRegistrationId(),
                                    providerIdText
                            )
                            .map(User::getId)
                            .orElse(null);
                }
            }
        }

        // 로컬 인증 계정도 loginId 우선, userName 보조 조회로 sellerId를 찾게 맞춤 -3/18
        return userRepository.findByLoginId(authentication.getName())
                .or(() -> userRepository.findByUserName(authentication.getName()))
                .map(User::getId)
                .orElse(null);
    }
}
