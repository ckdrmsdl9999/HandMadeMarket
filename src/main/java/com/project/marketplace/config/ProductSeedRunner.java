package com.project.marketplace.config;

import com.project.marketplace.product.entity.Product;
import com.project.marketplace.product.repository.ProductRepository;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductSeedRunner implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String LOCAL_PROVIDER = "local";
    private static final String TEST_PASSWORD = "1234";

    private static final List<SeedUser> SEED_USERS = List.of(
            // 관리자 화면과 권한 수정 흐름을 바로 확인할 수 있게 기본 관리자 계정을 추가함
            new SeedUser("admin_test", "관리자", "admin@test.com", UserRole.ADMIN),
            new SeedUser("artisan_lee", "이공방", "artisan_lee@test.com", UserRole.SELLER),
            new SeedUser("woven_kim", "김직조", "woven_kim@test.com", UserRole.SELLER),
            new SeedUser("buyer_test", "테스트 구매자", "buyer_test@test.com", UserRole.USER)
    );

    private static final List<SeedProduct> SEED_PRODUCTS = List.of(
            new SeedProduct("artisan_lee", "제주 감귤 비누 세트", "비누", 30, 12000, "제주 감귤 향을 담은 수제 비누 세트"),
            new SeedProduct("artisan_lee", "호두나무 우드 트레이", "목공", 12, 32000, "호두나무 원목으로 만든 핸드메이드 트레이"),
            new SeedProduct("woven_kim", "쪽빛 손염색 스카프", "패브릭", 20, 28000, "천연 쪽빛으로 손염색한 가벼운 스카프"),
            // 문구 검색 결과를 여러 건으로 확인할 수 있게 테스트 상품 추가함
            new SeedProduct("woven_kim", "한지 학종이", "문구", 40, 8000, "한지로 만든 학종이"),
            new SeedProduct("woven_kim", "일반 노트", "문구", 40, 8000, "일반 노트"),
            new SeedProduct("woven_kim", "한지 커버 미니 노트", "문구", 40, 8000, "한지 커버를 사용한 작은 수제 노트")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 테스트 로그인과 상품 등록 흐름을 바로 확인할 수 있도록 기본 사용자와 상품을 보장함
        SEED_USERS.forEach(this::ensureUser);
        SEED_PRODUCTS.forEach(this::ensureProduct);
    }

    private User ensureUser(SeedUser seedUser) {
        return userRepository.findByProviderAndLoginId(LOCAL_PROVIDER, seedUser.loginId())
                .orElseGet(() -> userRepository.save(User.builder()
                        .loginId(seedUser.loginId())
                        .password(passwordEncoder.encode(TEST_PASSWORD))
                        .role(seedUser.role())
                        .provider(LOCAL_PROVIDER)
                        .userName(seedUser.userName())
                        .email(seedUser.email())
                        .build()));
    }

    private void ensureProduct(SeedProduct seedProduct) {
        User seller = userRepository.findByProviderAndLoginId(LOCAL_PROVIDER, seedProduct.sellerLoginId())
                .orElseThrow(() -> new IllegalStateException("테스트 판매자를 찾을 수 없습니다. ID: " + seedProduct.sellerLoginId()));

        if (productRepository.existsByNameAndSeller_Id(seedProduct.name(), seller.getId())) {
            return;
        }

        productRepository.save(Product.builder()
                .name(seedProduct.name())
                .category(seedProduct.category())
                .quantity(seedProduct.quantity())
                .salesCount(0)
                .price(seedProduct.price())
                .description(seedProduct.description())
                .isSoldOut(seedProduct.quantity() <= 0)
                .seller(seller)
                .build());
    }

    private record SeedUser(String loginId, String userName, String email, UserRole role) {
    }

    private record SeedProduct(String sellerLoginId, String name, String category, Integer quantity, Integer price, String description) {
    }
}
