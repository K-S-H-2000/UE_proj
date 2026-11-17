package kr.ac.tukorea.ue.domain.product.service;

import kr.ac.tukorea.ue.domain.comment.repository.CommentRepository;
import kr.ac.tukorea.ue.domain.interaction.repository.BlockRepository;
import kr.ac.tukorea.ue.domain.interaction.repository.FavoriteRepository;
import kr.ac.tukorea.ue.domain.product.dto.ProductDto;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.entity.ProductCondition;
import kr.ac.tukorea.ue.domain.product.entity.ProductStatus;
import kr.ac.tukorea.ue.domain.product.repository.ProductRepository;
import kr.ac.tukorea.ue.domain.search.repository.SynonymRepository;
import kr.ac.tukorea.ue.domain.transaction.repository.TransactionRepository;
import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.dto.PageResponse;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private SynonymRepository synonymRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProductService productService;

    private University university;
    private User seller;
    private Product product;

    @BeforeEach
    void setUp() {
        university = University.builder()
                .name("한국공학대학교")
                .emailDomain("tukorea.ac.kr")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(university, "id", 1L);

        seller = User.builder()
                .university(university)
                .email("seller@tukorea.ac.kr")
                .nickname("판매자")
                .build();
        ReflectionTestUtils.setField(seller, "id", 10L);

        product = Product.builder()
                .university(university)
                .seller(seller)
                .title("공학용 계산기")
                .department("전자공학")
                .price(15000)
                .condition(ProductCondition.OPENED)
                .description("상태 좋아요")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", 100L);
    }

    @Test
    @DisplayName("4-1. 홈 피드 조회 - 차단 목록이 없는 경우 정상 페이징 조회")
    void getFeed_noBlockedUsers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);

        given(blockRepository.findAllMutualBlockedUserIds(1L)).willReturn(Collections.emptyList());
        given(productRepository.findFeedWithoutExclusion(eq(1L), anyCollection(), isNull(), eq(pageable)))
                .willReturn(page);
        given(commentRepository.countByProductIdAndDeletedAtIsNull(100L)).willReturn(3);

        PageResponse<ProductDto.FeedResponse> response = productService.getFeed(1L, 1L, null, "ON_SALE", pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("공학용 계산기");
        assertThat(response.getContent().get(0).getCommentCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("4-3. 상품 상세 조회 - 타 대학 상품 조회 시 NOT_FOUND 예외")
    void getDetail_differentUniversity() {
        University otherUniv = University.builder().name("타대학").emailDomain("other.ac.kr").build();
        ReflectionTestUtils.setField(otherUniv, "id", 2L);
        ReflectionTestUtils.setField(product, "university", otherUniv);

        given(productRepository.findById(100L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.getDetail(1L, 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("4-3. 상품 상세 조회 - 차단된 사용자의 상품이면 BLOCKED_RELATION 예외")
    void getDetail_blockedRelation() {
        given(productRepository.findById(100L)).willReturn(Optional.of(product));
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> productService.getDetail(1L, 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLOCKED_RELATION);
    }

    @Test
    @DisplayName("4-4. 상품 등록 시 이미지가 10장 초과이면 IMAGE_LIMIT_EXCEEDED 예외")
    void createProduct_imageLimitExceeded() {
        List<String> tooManyImages = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            tooManyImages.add("img" + i);
        }

        ProductDto.CreateRequest request = new ProductDto.CreateRequest();
        ReflectionTestUtils.setField(request, "title", "제목");

        assertThatThrownBy(() -> productService.createProduct(1L, 10L, request, tooManyImages))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("4-5. 타인이 상품 수정을 시도하면 FORBIDDEN 예외")
    void updateProduct_notSeller() {
        given(productRepository.findById(100L)).willReturn(Optional.of(product));

        ProductDto.UpdateRequest request = new ProductDto.UpdateRequest();
        ReflectionTestUtils.setField(request, "title", "새제목");

        assertThatThrownBy(() -> productService.updateProduct(999L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("4-5. 예약 진행 중인 상품 삭제 시 CANNOT_DELETE_RESERVED_PRODUCT 예외")
    void deleteProduct_reserved() {
        product.updateStatus(ProductStatus.RESERVED);
        given(productRepository.findById(100L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.deleteProduct(10L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_DELETE_RESERVED_PRODUCT);
    }

    @Test
    @DisplayName("4-5. 판매완료된 상품 삭제 시 CANNOT_DELETE_SOLD_PRODUCT 예외")
    void deleteProduct_sold() {
        product.updateStatus(ProductStatus.SOLD);
        given(productRepository.findById(100L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.deleteProduct(10L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_DELETE_SOLD_PRODUCT);
    }

    @Test
    @DisplayName("4-5. 판매중(ON_SALE) 상품은 정상 삭제")
    void deleteProduct_success() {
        given(productRepository.findById(100L)).willReturn(Optional.of(product));

        productService.deleteProduct(10L, 100L);

        verify(productRepository).delete(product);
    }
}
