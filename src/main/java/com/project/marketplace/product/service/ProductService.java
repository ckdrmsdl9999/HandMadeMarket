package com.project.marketplace.product.service;

import com.project.marketplace.product.dto.ProductDto;
import com.project.marketplace.product.entity.Product;
import com.project.marketplace.product.repository.ProductRepository;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        // DTO 변환에서 seller 정보를 바로 쓰므로 fetch join 조회로 판매자 추가 조회를 막음
        return productRepository.findAllWithSeller()
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId) {
        // Order 서비스와 조회 실패 처리 규칙을 맞추기 위해 예외 대신 null을 반환하도록 변경했다.
        return productRepository.findById(productId)
                .map(ProductDto::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategory(category)
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> searchProductsByName(String keyword) {
        return productRepository.findByNameContaining(keyword)
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 판매자 관리 화면에서 현재 사용자 상품만 조회하도록함
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsBySellerId(Long sellerId) {
        return productRepository.findProductsBySellerId(sellerId)
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createProduct(ProductDto dto) {
        // 상품 등록 시 sellerId로 사용자 엔티티를 연결해 상품-판매자 FK가 누락되지 않도록 했다.
        if (dto.getSellerId() == null) {
            throw new RuntimeException("판매자 ID는 필수입니다.");
        }
        // sellerId를 실제 사용자 엔티티로 조회해 연관관계 무결성을 보장한다.
        User seller = userRepository.findById(dto.getSellerId())
                .orElseThrow(() -> new RuntimeException("판매자를 찾을 수 없습니다. ID: " + dto.getSellerId()));

        Product product = ProductDto.toEntity(dto);
        if (product.getSalesCount() == null) product.setSalesCount(0);
        if (product.getQuantity() == null) product.setQuantity(0);
        product.setIsSoldOut(product.getQuantity() <= 0);
        // 생성 시점에 판매자 연관관계를 설정해 추후 사용자 기준 상품 조회가 가능하도록 했다.
        product.setSeller(seller);
        return productRepository.save(product).getId();
    }

    @Transactional
    public void updateProduct(ProductDto dto, Long requesterUserId) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품을 찾을 수 없습니다."));
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        // 상품 수정은 관리자 또는 상품 판매자 본인만 가능하도록 제한함
        validateProductManager(product, requester, "본인 상품만 수정할 수 있습니다.");

        product.setName(dto.getProductName());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setQuantity(dto.getQuantity());
        product.setDescription(dto.getMainImage());
        product.setIsSoldOut(dto.getQuantity() <= 0);
        // 수정 요청의 sellerId는 상품 소유자 변경으로 악용될 수 있어 반영하지 않음

        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long productId, Long requesterUserId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품을 찾을 수 없습니다."));
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        // 상품 삭제도 수정과 같은 관리자 또는 판매자 본인 기준을 재사용함
        validateProductManager(product, requester, "본인 상품만 삭제할 수 있습니다.");

        productRepository.delete(product);
    }

    @Transactional
    public void updateProductQuantity(Long productId, Integer quantityChange, Long requesterUserId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품을 찾을 수 없습니다."));
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        // 재고 변경도 상품 수정 권한과 같은 관리자 또는 판매자 본인 기준을 적용함
        validateProductManager(product, requester, "본인 상품만 수정할 수 있습니다.");
        applyQuantityChange(product, quantityChange);
    }

    @Transactional
    public void updateProductQuantity(Long productId, Integer quantityChange) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. ID: " + productId));

        // 구매 처리에서는 주문 흐름에서 검증하므로 재고 증감만 공통 처리함
        applyQuantityChange(product, quantityChange);
    }

    private void applyQuantityChange(Product product, Integer quantityChange) {
        // 재고 변경 계산을 한 곳으로 모아 판매자 수정과 구매 차감 흐름을 동일하게 처리함
        if (quantityChange < 0 && product.getQuantity() < -quantityChange) {
            throw new RuntimeException("상품 재고가 부족합니다.");
        }

        product.setQuantity(product.getQuantity() + quantityChange);
        product.setIsSoldOut(product.getQuantity() <= 0);
        productRepository.save(product);
    }

    private void validateProductManager(Product product, User requester, String forbiddenMessage) {
        // 관리자이거나 상품 판매자 본인인지 확인해 다른 판매자 상품 변경을 막음
        boolean canManage = requester.getRole() == UserRole.ADMIN
                || (product.getSeller() != null && requester.getId().equals(product.getSeller().getId()));
        if (!canManage) {
            throw new ResponseStatusException(FORBIDDEN, forbiddenMessage);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getPopularProducts(int limit) {
        return productRepository.findPopularProducts(PageRequest.of(0, limit))
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void purchaseProduct(Long productId, Integer quantity) {
        // 바로 구매도 조건부 update로 재고 차감과 판매 수량 증가를 원자적으로 처리함
        int updatedRows = productRepository.decreaseStockIfEnough(productId, quantity);
        if (updatedRows == 0) {
            throw new RuntimeException("상품 재고가 부족합니다.");
        }
    }
}
