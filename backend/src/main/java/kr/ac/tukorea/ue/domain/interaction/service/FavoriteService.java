package kr.ac.tukorea.ue.domain.interaction.service;

import kr.ac.tukorea.ue.domain.comment.repository.CommentRepository;
import kr.ac.tukorea.ue.domain.interaction.dto.FavoriteDto;
import kr.ac.tukorea.ue.domain.interaction.entity.Favorite;
import kr.ac.tukorea.ue.domain.interaction.repository.FavoriteRepository;
import kr.ac.tukorea.ue.domain.product.dto.ProductDto;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.service.ProductService;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductService productService;
    private final UserService userService;
    private final CommentRepository commentRepository;

    /**
     * 5-1. 찜 토글 (등록/해제)
     */
    @Transactional
    public FavoriteDto.Response toggleFavorite(Long userId, Long productId) {
        Product product = productService.findProductById(productId);
        User user = userService.findUserById(userId);

        boolean alreadyFavorited = favoriteRepository.existsByUserIdAndProductId(userId, productId);

        if (alreadyFavorited) {
            favoriteRepository.deleteByUserIdAndProductId(userId, productId);
            product.decrementFavoriteCount();
            return new FavoriteDto.Response(false, product.getFavoriteCount());
        } else {
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .product(product)
                    .build();
            favoriteRepository.save(favorite);
            product.incrementFavoriteCount();
            return new FavoriteDto.Response(true, product.getFavoriteCount());
        }
    }

    /**
     * 5-2. 찜 목록 조회
     */
    public PageResponse<ProductDto.FeedResponse> getMyFavorites(Long userId, Pageable pageable) {
        Page<Favorite> favoritePage = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        Page<ProductDto.FeedResponse> dtoPage = favoritePage.map(fav -> {
            Product p = fav.getProduct();
            int commentCount = commentRepository.countByProductIdAndDeletedAtIsNull(p.getId());
            return ProductDto.FeedResponse.from(p, commentCount);
        });

        return PageResponse.from(dtoPage);
    }
}
