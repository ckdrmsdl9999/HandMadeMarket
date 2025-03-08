package com.project.marketplace.product.controller;

import com.project.marketplace.product.dto.ProductDto;
import com.project.marketplace.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 모든 상품 목록을 조회합니다.
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
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
    public ResponseEntity<Map<String, Long>> createProduct(@RequestBody ProductDto productDto) {
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

        ProductDto existingProduct = productService.getProductById(productId);
        if (existingProduct == null) {
            return ResponseEntity.notFound().build();
        }

        productDto.setProductId(productId);
        productService.updateProduct(productDto);

        return ResponseEntity.ok().build();
    }

    /**
     * 상품을 삭제합니다.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        ProductDto existingProduct = productService.getProductById(productId);
        if (existingProduct == null) {
            return ResponseEntity.notFound().build();
        }

        productService.deleteProduct(productId);
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
}
