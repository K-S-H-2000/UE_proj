package kr.ac.tukorea.ue.domain.product.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "department", length = 50, nullable = false)
    private String department;

    @Column(name = "price", nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", length = 20, nullable = false)
    private ProductCondition condition;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProductStatus status = ProductStatus.ON_SALE;

    @Column(name = "favorite_count", nullable = false)
    private int favoriteCount = 0;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @Builder
    public Product(University university, User seller, String title, String department,
                   int price, ProductCondition condition, String description,
                   ProductStatus status, Integer favoriteCount) {
        this.university = university;
        this.seller = seller;
        this.title = title;
        this.department = department;
        this.price = price;
        this.condition = condition;
        this.description = description;
        this.status = (status != null) ? status : ProductStatus.ON_SALE;
        this.favoriteCount = (favoriteCount != null) ? favoriteCount : 0;
    }

    public void addImage(ProductImage image) {
        this.images.add(image);
        image.assignProduct(this);
    }

    public void removeImage(ProductImage image) {
        this.images.remove(image);
    }

    public void update(String title, String department, int price, ProductCondition condition, String description) {
        this.title = title;
        this.department = department;
        this.price = price;
        this.condition = condition;
        this.description = description;
    }

    public void updateStatus(ProductStatus status) {
        this.status = status;
    }

    public void incrementFavoriteCount() {
        this.favoriteCount++;
    }

    public void decrementFavoriteCount() {
        if (this.favoriteCount > 0) {
            this.favoriteCount--;
        }
    }
}
