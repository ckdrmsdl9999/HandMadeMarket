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

import java.util.List;


@Component
@RequiredArgsConstructor
public class ProductSeedRunner implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }

        User sellerA = getOrCreateSeller("artisan_lee", "artisan_lee", "artisan.lee@local.test");
        User sellerB = getOrCreateSeller("woven_kim", "woven_kim", "woven.kim@local.test");

        productRepository.saveAll(List.of(
                Product.builder()
                        .name("제주 감귤 비누 세트")
                        .category("뷰티")
                        .quantity(30)
                        .salesCount(18)
                        .price(12900)
                        .description("제주 감귤 오일을 넣어 상큼하게 만든 수제 비누 3종 세트")
                        .isSoldOut(false)
                        .seller(sellerA)
                        .build(),
                Product.builder()
                        .name("호두나무 우드 트레이")
                        .category("주방")
                        .quantity(12)
                        .salesCount(11)
                        .price(28000)
                        .description("티타임과 브런치 플레이팅에 어울리는 핸드메이드 우드 트레이")
                        .isSoldOut(false)
                        .seller(sellerA)
                        .build(),
                Product.builder()
                        .name("쪽빛 손염색 스카프")
                        .category("패션")
                        .quantity(20)
                        .salesCount(15)
                        .price(24500)
                        .description("자연 염색으로 완성한 얇고 가벼운 사계절용 코튼 스카프")
                        .isSoldOut(false)
                        .seller(sellerB)
                        .build(),
                Product.builder()
                        .name("도자기 머그컵 2인 세트")
                        .category("리빙")
                        .quantity(8)
                        .salesCount(9)
                        .price(32000)
                        .description("유약 색감이 다른 수제 머그컵 두 점으로 구성한 선물용 세트")
                        .isSoldOut(false)
                        .seller(sellerB)
                        .build(),
                Product.builder()
                        .name("한지 커버 미니 노트")
                        .category("문구")
                        .quantity(25)
                        .salesCount(6)
                        .price(8900)
                        .description("한지 표지와 실 제본으로 마감한 휴대용 핸드메이드 노트")
                        .isSoldOut(false)
                        .seller(sellerA)
                        .build()
        ));
    }


    private User getOrCreateSeller(String loginId, String userName, String email) {
        return userRepository.findByLoginId(loginId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .loginId(loginId)
                        .userName(userName)
                        .password(passwordEncoder.encode("seed1234"))
                        .role(UserRole.SELLER)
                        .email(email)
                        .provider("local")
                        .build()));
    }
}
