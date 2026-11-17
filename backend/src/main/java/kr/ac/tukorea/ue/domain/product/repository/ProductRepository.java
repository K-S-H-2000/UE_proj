package kr.ac.tukorea.ue.domain.product.repository;

import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p " +
           "WHERE p.university.id = :universityId " +
           "AND p.status IN :statuses " +
           "AND (:department IS NULL OR p.department = :department) " +
           "AND (p.seller.id NOT IN :excludedSellerIds) " +
           "ORDER BY p.createdAt DESC")
    Page<Product> findFeed(
            @Param("universityId") Long universityId,
            @Param("statuses") Collection<ProductStatus> statuses,
            @Param("department") String department,
            @Param("excludedSellerIds") Collection<Long> excludedSellerIds,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p " +
           "WHERE p.university.id = :universityId " +
           "AND p.status IN :statuses " +
           "AND (:department IS NULL OR p.department = :department) " +
           "ORDER BY p.createdAt DESC")
    Page<Product> findFeedWithoutExclusion(
            @Param("universityId") Long universityId,
            @Param("statuses") Collection<ProductStatus> statuses,
            @Param("department") String department,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p " +
           "WHERE p.university.id = :universityId " +
           "AND p.status IN :statuses " +
           "AND (:department IS NULL OR p.department = :department) " +
           "AND (p.title LIKE %:keyword% OR p.description LIKE %:keyword%) " +
           "ORDER BY p.createdAt DESC")
    Page<Product> searchByKeyword(
            @Param("universityId") Long universityId,
            @Param("statuses") Collection<ProductStatus> statuses,
            @Param("department") String department,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countBySellerIdAndStatus(Long sellerId, ProductStatus status);

    Page<Product> findBySellerIdAndStatus(Long sellerId, ProductStatus status, Pageable pageable);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);
}
