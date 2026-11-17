package kr.ac.tukorea.ue.domain.product.controller;

import jakarta.validation.Valid;
import kr.ac.tukorea.ue.domain.interaction.dto.FavoriteDto;
import kr.ac.tukorea.ue.domain.interaction.service.FavoriteService;
import kr.ac.tukorea.ue.domain.product.dto.ProductDto;
import kr.ac.tukorea.ue.domain.product.entity.ProductStatus;
import kr.ac.tukorea.ue.domain.product.service.ProductService;
import kr.ac.tukorea.ue.global.dto.PageResponse;
import kr.ac.tukorea.ue.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final FavoriteService favoriteService;

    /**
     * 4-1. 홈 피드 조회
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductDto.FeedResponse>> getFeed(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String department,
            @RequestParam(required = false, defaultValue = "ON_SALE") String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ProductDto.FeedResponse> response = productService.getFeed(
                userPrincipal.getUniversityId(), userPrincipal.getUserId(), department, status, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 4-2. 상품 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ProductDto.SearchResponse> search(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam String keyword,
            @RequestParam(required = false) String department,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        ProductDto.SearchResponse response = productService.search(
                userPrincipal.getUniversityId(), userPrincipal.getUserId(), keyword, department, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 4-3. 상품 상세 조회
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto.DetailResponse> getDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId
    ) {
        ProductDto.DetailResponse response = productService.getDetail(
                userPrincipal.getUniversityId(), userPrincipal.getUserId(), productId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 4-4. 상품 등록 (JSON + 이미지 파일 지원)
     */
    @PostMapping
    public ResponseEntity<ProductDto.CreateResponse> createProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart("data") @Valid ProductDto.CreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> files
    ) {
        List<String> imageUrls = new ArrayList<>();
        if (files != null) {
            int order = 1;
            for (MultipartFile file : files) {
                // S3 업로드 시뮬레이션
                imageUrls.add("https://ue-bucket.s3.ap-northeast-2.amazonaws.com/products/"
                        + System.currentTimeMillis() + "_" + (order++) + ".jpg");
            }
        }

        ProductDto.CreateResponse response = productService.createProduct(
                userPrincipal.getUniversityId(), userPrincipal.getUserId(), request, imageUrls
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 4-5. 상품 수정
     */
    @PatchMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId,
            @Valid @RequestBody ProductDto.UpdateRequest request
    ) {
        productService.updateProduct(userPrincipal.getUserId(), productId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 4-5. 상품 삭제
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId
    ) {
        productService.deleteProduct(userPrincipal.getUserId(), productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 4-6. 내 판매글 목록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ProductDto.MyProductListResponse> getMyProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) ProductStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        ProductDto.MyProductListResponse response = productService.getMyProducts(
                userPrincipal.getUserId(), status, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 4-7. AI 판매글 초안 생성
     */
    @PostMapping("/ai-draft")
    public ResponseEntity<ProductDto.AiDraftResponse> generateAiDraft(
            @RequestParam("images") List<MultipartFile> files
    ) {
        List<String> urls = files != null ? files.stream().map(MultipartFile::getOriginalFilename).toList() : List.of();
        ProductDto.AiDraftResponse response = productService.generateAiDraft(urls);
        return ResponseEntity.ok(response);
    }

    /**
     * 5-1. 찜 등록 / 해제 (토글)
     */
    @PostMapping("/{productId}/favorite")
    public ResponseEntity<FavoriteDto.Response> addFavorite(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId
    ) {
        FavoriteDto.Response response = favoriteService.toggleFavorite(userPrincipal.getUserId(), productId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}/favorite")
    public ResponseEntity<FavoriteDto.Response> removeFavorite(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId
    ) {
        FavoriteDto.Response response = favoriteService.toggleFavorite(userPrincipal.getUserId(), productId);
        return ResponseEntity.ok(response);
    }
}
