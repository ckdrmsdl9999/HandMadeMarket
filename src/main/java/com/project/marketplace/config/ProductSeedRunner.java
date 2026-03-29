package com.project.marketplace.config;

import com.project.marketplace.product.repository.ProductRepository;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class ProductSeedRunner implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // 초기 샘플 상품만 골라 비워 새로 등록한 상품만 화면에 남게 맞춤
    private static final List<String> SEED_PRODUCT_NAMES = List.of(
            "제주 감귤 비누 세트",
            "호두나무 우드 트레이",
            "쪽빛 손염색 스카프",
            "한지 커버 미니 노트"
    );

    // 샘플 판매자 계정 기준으로만 정리해 실제 사용자 상품은 건드리지 않게 맞춤
    private static final List<String> SEED_SELLER_LOGIN_IDS = List.of(
            "artisan_lee",
            "woven_kim"
    );


    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 서버 시작 시 기존 초기 상품을 제거해 이후에는 직접 추가한 상품만 보이게 맞춤
        List<Long> seedSellerIds = SEED_SELLER_LOGIN_IDS.stream()
                .map(userRepository::findByLoginId)
                .flatMap(Optional::stream)
                .map(User::getId)
                .toList();

        if (seedSellerIds.isEmpty()) {
            return;
        }

        productRepository.deleteByNameInAndSeller_IdIn(SEED_PRODUCT_NAMES, seedSellerIds);
    }
}
