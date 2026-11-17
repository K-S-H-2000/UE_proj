package kr.ac.tukorea.ue.domain.interaction.service;

import kr.ac.tukorea.ue.domain.comment.repository.CommentRepository;
import kr.ac.tukorea.ue.domain.interaction.dto.FavoriteDto;
import kr.ac.tukorea.ue.domain.interaction.entity.Favorite;
import kr.ac.tukorea.ue.domain.interaction.repository.FavoriteRepository;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.service.ProductService;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = User.builder().nickname("테스터").email("test@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        product = Product.builder().title("상품").price(1000).favoriteCount(0).build();
        ReflectionTestUtils.setField(product, "id", 100L);
    }

    @Test
    @DisplayName("5-1. 찜하지 않은 상품 토글 시 찜 생성 및 카운트 +1")
    void toggleFavorite_add() {
        given(productService.findProductById(100L)).willReturn(product);
        given(userService.findUserById(1L)).willReturn(user);
        given(favoriteRepository.existsByUserIdAndProductId(1L, 100L)).willReturn(false);

        FavoriteDto.Response response = favoriteService.toggleFavorite(1L, 100L);

        assertThat(response.isFavorited()).isTrue();
        assertThat(response.getFavoriteCount()).isEqualTo(1);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("5-1. 이미 찜한 상품 토글 시 찜 삭제 및 카운트 -1")
    void toggleFavorite_remove() {
        ReflectionTestUtils.setField(product, "favoriteCount", 1);
        given(productService.findProductById(100L)).willReturn(product);
        given(userService.findUserById(1L)).willReturn(user);
        given(favoriteRepository.existsByUserIdAndProductId(1L, 100L)).willReturn(true);

        FavoriteDto.Response response = favoriteService.toggleFavorite(1L, 100L);

        assertThat(response.isFavorited()).isFalse();
        assertThat(response.getFavoriteCount()).isEqualTo(0);
        verify(favoriteRepository).deleteByUserIdAndProductId(1L, 100L);
    }
}
