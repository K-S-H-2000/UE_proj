package kr.ac.tukorea.ue.domain.product.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "s3_url", length = 500, nullable = false)
    private String s3Url;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;

    @Builder
    public ProductImage(Product product, String s3Url, Short sortOrder) {
        this.product = product;
        this.s3Url = s3Url;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    void assignProduct(Product product) {
        this.product = product;
    }

    public void updateSortOrder(short sortOrder) {
        this.sortOrder = sortOrder;
    }
}
