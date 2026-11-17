package kr.ac.tukorea.ue.domain.product.service;

import kr.ac.tukorea.ue.domain.comment.repository.CommentRepository;
import kr.ac.tukorea.ue.domain.interaction.repository.BlockRepository;
import kr.ac.tukorea.ue.domain.interaction.repository.FavoriteRepository;
import kr.ac.tukorea.ue.domain.product.dto.ProductDto;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.entity.ProductImage;
import kr.ac.tukorea.ue.domain.product.entity.ProductStatus;
import kr.ac.tukorea.ue.domain.product.repository.ProductRepository;
import kr.ac.tukorea.ue.domain.search.entity.Synonym;
import kr.ac.tukorea.ue.domain.search.repository.SynonymRepository;
import kr.ac.tukorea.ue.domain.transaction.entity.Transaction;
import kr.ac.tukorea.ue.domain.transaction.entity.TransactionStatus;
import kr.ac.tukorea.ue.domain.transaction.repository.TransactionRepository;
import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.dto.PageResponse;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final BlockRepository blockRepository;
    private final SynonymRepository synonymRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    private static final int MAX_IMAGE_COUNT = 10;

    /**
     * 4-1. 홈 피드 조회
     */
    public PageResponse<ProductDto.FeedResponse> getFeed(
            Long universityId, Long userId, String department, String statusStr, Pageable pageable
    ) {
        List<ProductStatus> statuses = determineStatuses(statusStr);
        List<Long> blockedUserIds = blockRepository.findAllMutualBlockedUserIds(userId);

        Page<Product> productPage;
        if (blockedUserIds.isEmpty()) {
            productPage = productRepository.findFeedWithoutExclusion(
                    universityId, statuses, department, pageable
            );
        } else {
            productPage = productRepository.findFeed(
                    universityId, statuses, department, blockedUserIds, pageable
            );
        }

        Page<ProductDto.FeedResponse> dtoPage = productPage.map(p -> {
            int commentCount = commentRepository.countByProductIdAndDeletedAtIsNull(p.getId());
            return ProductDto.FeedResponse.from(p, commentCount);
        });

        return PageResponse.from(dtoPage);
    }

    /**
     * 4-2. 상품 검색 (동의어 확장)
     */
    public ProductDto.SearchResponse search(
            Long universityId, Long userId, String keyword, String department, Pageable pageable
    ) {
        List<String> expandedKeywords = new ArrayList<>();
        expandedKeywords.add(keyword);

        List<Synonym> synonyms = synonymRepository.findByKeyword(keyword);
        for (Synonym s : synonyms) {
            expandedKeywords.add(s.getSynonym());
        }

        List<ProductStatus> statuses = List.of(ProductStatus.ON_SALE, ProductStatus.RESERVED);

        Page<Product> productPage = productRepository.searchByKeyword(
                universityId, statuses, department, keyword, pageable
        );

        List<ProductDto.FeedResponse> feedList = productPage.getContent().stream()
                .map(p -> {
                    int commentCount = commentRepository.countByProductIdAndDeletedAtIsNull(p.getId());
                    return ProductDto.FeedResponse.from(p, commentCount);
                })
                .toList();

        return ProductDto.SearchResponse.builder()
                .content(feedList)
                .expandedKeywords(expandedKeywords)
                .totalElements(productPage.getTotalElements())
                .build();
    }

    /**
     * 4-3. 상품 상세 조회
     */
    public ProductDto.DetailResponse getDetail(Long universityId, Long userId, Long productId) {
        Product product = findProductById(productId);

        if (!product.getUniversity().getId().equals(universityId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 학교의 상품이 아니에요.");
        }

        if (blockRepository.existsByBlockerIdAndBlockedId(userId, product.getSeller().getId()) ||
            blockRepository.existsByBlockerIdAndBlockedId(product.getSeller().getId(), userId)) {
            throw new BusinessException(ErrorCode.BLOCKED_RELATION);
        }

        boolean isFavorited = favoriteRepository.existsByUserIdAndProductId(userId, productId);
        boolean isMine = product.getSeller().getId().equals(userId);

        return ProductDto.DetailResponse.from(product, isFavorited, isMine);
    }

    /**
     * 4-4. 상품 등록
     */
    @Transactional
    public ProductDto.CreateResponse createProduct(
            Long universityId, Long userId, ProductDto.CreateRequest request, List<String> imageUrls
    ) {
        if (imageUrls != null && imageUrls.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        User seller = userService.findUserById(userId);
        University university = seller.getUniversity();

        Product product = Product.builder()
                .university(university)
                .seller(seller)
                .title(request.getTitle())
                .department(request.getDepartment())
                .price(request.getPrice())
                .condition(request.getCondition())
                .description(request.getDescription())
                .status(ProductStatus.ON_SALE)
                .favoriteCount(0)
                .build();

        if (imageUrls != null) {
            short order = 0;
            for (String url : imageUrls) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .s3Url(url)
                        .sortOrder(order++)
                        .build();
                product.addImage(image);
            }
        }

        Product savedProduct = productRepository.save(product);
        return ProductDto.CreateResponse.builder()
                .productId(savedProduct.getId())
                .build();
    }

    /**
     * 4-5. 상품 수정
     */
    @Transactional
    public void updateProduct(Long userId, Long productId, ProductDto.UpdateRequest request) {
        Product product = findProductById(productId);

        if (!product.getSeller().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 판매글만 수정할 수 있어요.");
        }

        product.update(
                request.getTitle() != null ? request.getTitle() : product.getTitle(),
                request.getDepartment() != null ? request.getDepartment() : product.getDepartment(),
                request.getPrice() != null ? request.getPrice() : product.getPrice(),
                request.getCondition() != null ? request.getCondition() : product.getCondition(),
                request.getDescription() != null ? request.getDescription() : product.getDescription()
        );
    }

    /**
     * 4-5. 상품 삭제
     * - ON_SALE: 삭제 가능
     * - RESERVE_REQUESTED, RESERVED: 삭제 불가 (CANNOT_DELETE_RESERVED_PRODUCT)
     * - SOLD: 삭제 불가 (CANNOT_DELETE_SOLD_PRODUCT)
     */
    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        Product product = findProductById(productId);

        if (!product.getSeller().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 판매글만 삭제할 수 있어요.");
        }

        if (product.getStatus() == ProductStatus.RESERVE_REQUESTED || product.getStatus() == ProductStatus.RESERVED) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_RESERVED_PRODUCT);
        }

        if (product.getStatus() == ProductStatus.SOLD) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SOLD_PRODUCT);
        }

        productRepository.delete(product);
    }

    /**
     * 4-6. 내 판매글 목록 조회
     */
    public ProductDto.MyProductListResponse getMyProducts(
            Long userId, ProductStatus filterStatus, Pageable pageable
    ) {
        long onSaleCount = productRepository.countBySellerIdAndStatus(userId, ProductStatus.ON_SALE)
                + productRepository.countBySellerIdAndStatus(userId, ProductStatus.RESERVE_REQUESTED);
        long reservedCount = productRepository.countBySellerIdAndStatus(userId, ProductStatus.RESERVED);
        long soldCount = productRepository.countBySellerIdAndStatus(userId, ProductStatus.SOLD);

        Map<String, Long> counts = new HashMap<>();
        counts.put("onSale", onSaleCount);
        counts.put("reserved", reservedCount);
        counts.put("sold", soldCount);

        Page<Product> productPage = (filterStatus != null)
                ? productRepository.findBySellerIdAndStatus(userId, filterStatus, pageable)
                : productRepository.findBySellerId(userId, pageable);

        List<ProductDto.MyProductResponse> content = productPage.getContent().stream()
                .map(p -> {
                    String thumb = p.getImages().isEmpty() ? null : p.getImages().get(0).getS3Url();
                    UserDto.SimpleUserResponse reservedWith = null;

                    if (p.getStatus() == ProductStatus.RESERVED) {
                        Optional<Transaction> activeTrx = transactionRepository.findByProductIdAndStatusIn(
                                p.getId(), List.of(TransactionStatus.RESERVED)
                        );
                        if (activeTrx.isPresent()) {
                            reservedWith = UserDto.SimpleUserResponse.from(activeTrx.get().getBuyer());
                        }
                    }

                    return ProductDto.MyProductResponse.builder()
                            .productId(p.getId())
                            .title(p.getTitle())
                            .price(p.getPrice())
                            .status(p.getStatus().name())
                            .thumbnailUrl(thumb)
                            .reservedWith(reservedWith)
                            .createdAt(p.getCreatedAt())
                            .build();
                })
                .toList();

        return ProductDto.MyProductListResponse.builder()
                .counts(counts)
                .content(content)
                .build();
    }

    /**
     * 4-7. AI 판매글 초안 생성
     */
    public ProductDto.AiDraftResponse generateAiDraft(List<String> imageUrls) {
        // AI Vision 모델 연동(또는 템플릿 반환)
        return ProductDto.AiDraftResponse.builder()
                .title("카시오 공학용 계산기 fx-570ES PLUS")
                .department("전자공학")
                .condition("OPENED")
                .description("한 학기 전공 수업에서 사용한 공학용 계산기입니다. 외관 깨끗하며 정상 작동합니다.")
                .build();
    }

    public Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없어요."));
    }

    private List<ProductStatus> determineStatuses(String statusStr) {
        if ("SOLD".equalsIgnoreCase(statusStr)) {
            return List.of(ProductStatus.SOLD);
        }
        // 기본값: ON_SALE (예약중 포함)
        return List.of(ProductStatus.ON_SALE, ProductStatus.RESERVED);
    }
}
