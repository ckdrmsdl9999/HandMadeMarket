package com.project.marketplace.cart.entity;

import com.project.marketplace.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items", uniqueConstraints = {//복합키
        @UniqueConstraint(name = "uk_cart_items_cart_product", columnNames = {"cart_id", "product_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;


    @Column(nullable = false)
    private Integer quantity;


    @Column(nullable = false)
    private Integer unitPriceSnapshot;


    @Column(nullable = false, length = 100)
    private String productNameSnapshot;


    @Column(nullable = false)
    private Integer lineAmount;


    public static CartItem create(Product product, Integer quantity) {
        return CartItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPriceSnapshot(product.getPrice())
                .productNameSnapshot(product.getName())
                .lineAmount(product.getPrice() * quantity)
                .build();
    }

    // 수량 변경 시 lineAmount도 즉시 재계산해 금액 불일치를 막는다.
    public void changeQuantity(Integer quantity) {
        this.quantity = quantity;
        this.lineAmount = this.unitPriceSnapshot * quantity;
    }
}
