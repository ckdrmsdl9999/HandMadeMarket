package com.project.marketplace.product.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category;

    private Integer quantity;

    private Integer salesCount;

    private Integer price;

    private String description;

    private Boolean isSoldOut;
}