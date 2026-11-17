package kr.ac.tukorea.ue.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.entity.ProductCondition;
import kr.ac.tukorea.ue.domain.product.entity.ProductImage;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProductDto {

    // === 요청 ===

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "제목은 필수예요.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없어요.")
        private String title;

        @NotBlank(message = "학과(카테고리)는 필수예요.")
        @Size(max = 50)
        private String department;

        @NotNull(message = "가격은 필수예요.")
        @Min(value = 0, message = "가격은 0원 이상이어야 해요.")
        private Integer price;

        @NotNull(message = "상품 상태는 필수예요.")
        private ProductCondition condition;

        private String description;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        @Size(max = 100, message = "제목은 100자를 초과할 수 없어요.")
        private String title;

        @Size(max = 50)
        private String department;

        @Min(value = 0, message = "가격은 0원 이상이어야 해요.")
        private Integer price;

        private ProductCondition condition;
        private String description;
    }

    // === 응답 ===

    @Getter
    @Builder
    public static class FeedResponse {
        private final Long productId;
        private final String title;
        private final int price;
        private final String department;
        private final String status;
        private final String thumbnailUrl;
        private final int commentCount;
        private final int favoriteCount;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime createdAt;

        public static FeedResponse from(Product product, int commentCount) {
            String thumbnail = product.getImages().isEmpty()
                    ? null
                    : product.getImages().get(0).getS3Url();
            return FeedResponse.builder()
                    .productId(product.getId())
                    .title(product.getTitle())
                    .price(product.getPrice())
                    .department(product.getDepartment())
                    .status(product.getStatus().name())
                    .thumbnailUrl(thumbnail)
                    .commentCount(commentCount)
                    .favoriteCount(product.getFavoriteCount())
                    .createdAt(product.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private final Long productId;
        private final String title;
        private final int price;
        private final String department;
        private final String condition;
        private final String description;
        private final String status;
        private final List<String> images;
        private final int favoriteCount;
        private final boolean isFavorited;
        private final UserDto.SellerResponse seller;
        private final boolean isMine;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime createdAt;

        public static DetailResponse from(Product product, boolean isFavorited, boolean isMine) {
            List<String> imageUrls = product.getImages().stream()
                    .map(ProductImage::getS3Url)
                    .toList();
            return DetailResponse.builder()
                    .productId(product.getId())
                    .title(product.getTitle())
                    .price(product.getPrice())
                    .department(product.getDepartment())
                    .condition(product.getCondition().name())
                    .description(product.getDescription())
                    .status(product.getStatus().name())
                    .images(imageUrls)
                    .favoriteCount(product.getFavoriteCount())
                    .isFavorited(isFavorited)
                    .seller(UserDto.SellerResponse.from(product.getSeller()))
                    .isMine(isMine)
                    .createdAt(product.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CreateResponse {
        private final Long productId;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MyProductResponse {
        private final Long productId;
        private final String title;
        private final int price;
        private final String status;
        private final String thumbnailUrl;
        private final UserDto.SimpleUserResponse reservedWith;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class MyProductListResponse {
        private final Map<String, Long> counts;
        private final List<MyProductResponse> content;
    }

    @Getter
    @Builder
    public static class AiDraftResponse {
        private final String title;
        private final String department;
        private final String condition;
        private final String description;
    }

    @Getter
    @Builder
    public static class SearchResponse {
        private final List<FeedResponse> content;
        private final List<String> expandedKeywords;
        private final long totalElements;
    }
}
