package com.project.marketplace.delivery.controller;

import com.project.marketplace.delivery.dto.DeliveryUpdateRequestDto;
import com.project.marketplace.delivery.dto.DeliveryUpdateResponseDto;
import com.project.marketplace.delivery.service.DeliveryService;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import com.project.marketplace.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery")
// Swagger 문서에서 배송 API를 도메인별로 묶기 위해 태그를 지정함
@Tag(name = "Delivery", description = "배송 API")
public class DeliveryController {
    private final DeliveryService deliveryService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<DeliveryUpdateResponseDto>> getAllDeliveries(Authentication authentication) {
        // 전체 배송 목록은 관리자만 조회
        getAdminUser(authentication);
        return ResponseEntity.ok(deliveryService.findAllDeliveryWithDto());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryUpdateResponseDto> getOneDelivery(@PathVariable Long id, Authentication authentication) {
        // 단건 배송 조회는 관리자 또는 해당 주문 사용자만 가능하도록 제한함
        User user = getAuthenticatedUser(authentication, "로그인 후 배송 정보를 조회할 수 있습니다.");
        boolean admin = user.getRole() == UserRole.ADMIN;
        return ResponseEntity.ok(deliveryService.findByIdDeliveryWithDto(id, user.getId(), admin));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryUpdateResponseDto> updateDelivery(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody DeliveryUpdateRequestDto requestDto) {
        // 배송 수정은 관리자 또는 해당 주문 상품의 판매자만 가능하도록 제한함
        User user = getDeliveryManager(authentication);
        boolean admin = user.getRole() == UserRole.ADMIN;
        return ResponseEntity.ok(deliveryService.updateDeliveryWithDto(id, user.getId(), admin, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelivery(@PathVariable Long id, Authentication authentication) {
        // 배송 삭제는 관리자만 허용함
        getAdminUser(authentication);
        deliveryService.deleteDelivery(id);
        return ResponseEntity.noContent().build();
    }

    // 인증 사용자 조회를 공통 처리해 일반 로그인과 OAuth2 로그인 모두 같은 기준을 사용함
    private User getAuthenticatedUser(Authentication authentication, String unauthorizedMessage) {
        return userService.getAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage));
    }

    // 배송 관리 API는 관리자만 접근하도록 공통 권한 검사를 사용함
    private User getAdminUser(Authentication authentication) {
        User user = getAuthenticatedUser(authentication, "로그인 후 이용할 수 있습니다.");
        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 이용할 수 있습니다.");
        }
        return user;
    }

    // 배송 수정 권한은 관리자와 판매자에게만 열고 실제 담당 판매자 여부는 서비스에서 검증함
    private User getDeliveryManager(Authentication authentication) {
        User user = getAuthenticatedUser(authentication, "로그인 후 이용할 수 있습니다.");
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "배송 수정 권한이 없습니다.");
        }
        return user;
    }
}
