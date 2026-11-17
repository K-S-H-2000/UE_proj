package kr.ac.tukorea.ue.domain.comment.service;

import kr.ac.tukorea.ue.domain.comment.dto.CommentDto;
import kr.ac.tukorea.ue.domain.comment.entity.Comment;
import kr.ac.tukorea.ue.domain.comment.entity.CommentLike;
import kr.ac.tukorea.ue.domain.comment.entity.CommentNotification;
import kr.ac.tukorea.ue.domain.comment.repository.CommentLikeRepository;
import kr.ac.tukorea.ue.domain.comment.repository.CommentNotificationRepository;
import kr.ac.tukorea.ue.domain.comment.repository.CommentRepository;
import kr.ac.tukorea.ue.domain.interaction.repository.BlockRepository;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.service.ProductService;
import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private CommentNotificationRepository commentNotificationRepository;

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @Mock
    private BlockRepository blockRepository;

    @InjectMocks
    private CommentService commentService;

    private University university;
    private User seller;
    private User buyer;
    private Product product;
    private Comment rootComment;

    @BeforeEach
    void setUp() {
        university = University.builder().name("한국공학대학교").emailDomain("tukorea.ac.kr").build();
        ReflectionTestUtils.setField(university, "id", 1L);

        seller = User.builder().university(university).nickname("판매자").email("seller@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(seller, "id", 10L);

        buyer = User.builder().university(university).nickname("구매자").email("buyer@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(buyer, "id", 20L);

        product = Product.builder().university(university).seller(seller).title("계산기").price(10000).build();
        ReflectionTestUtils.setField(product, "id", 100L);

        rootComment = Comment.builder().product(product).user(buyer).content("얼마까지 되나요?").offerPrice(8000).build();
        ReflectionTestUtils.setField(rootComment, "id", 500L);
    }

    @Test
    @DisplayName("6-1. 댓글 목록 조회 시 대댓글이 replies에 중첩되어 반환")
    void getComments_nestedReplies() {
        Comment reply = Comment.builder().product(product).user(seller).parent(rootComment).content("8000원은 힘들어요").build();
        ReflectionTestUtils.setField(reply, "id", 501L);

        given(productService.findProductById(100L)).willReturn(product);
        given(blockRepository.findAllMutualBlockedUserIds(20L)).willReturn(Collections.emptyList());
        given(commentRepository.findByProductIdOrderByCreatedAtAsc(100L)).willReturn(List.of(rootComment, reply));

        List<CommentDto.Response> responses = commentService.getComments(1L, 20L, 100L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCommentId()).isEqualTo(500L);
        assertThat(responses.get(0).getReplies()).hasSize(1);
        assertThat(responses.get(0).getReplies().get(0).getCommentId()).isEqualTo(501L);
        assertThat(responses.get(0).getReplies().get(0).isSeller()).isTrue(); // 판매자 배지
    }

    @Test
    @DisplayName("6-2. 대댓글에 다시 대댓글 작성 시 REPLY_DEPTH_EXCEEDED 예외 (2단계 제한)")
    void createComment_replyDepthExceeded() {
        Comment childReply = Comment.builder().product(product).user(seller).parent(rootComment).content("답글").build();
        ReflectionTestUtils.setField(childReply, "id", 501L);

        given(productService.findProductById(100L)).willReturn(product);
        given(commentRepository.findById(501L)).willReturn(Optional.of(childReply));
        given(userService.findUserById(20L)).willReturn(buyer);

        CommentDto.CreateRequest request = new CommentDto.CreateRequest();
        ReflectionTestUtils.setField(request, "parentCommentId", 501L);
        ReflectionTestUtils.setField(request, "content", "대댓글에 대댓글 달기");

        assertThatThrownBy(() -> commentService.createComment(1L, 20L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPLY_DEPTH_EXCEEDED);
    }

    @Test
    @DisplayName("6-2. 대댓글에 가격 제안 포함 시 OFFER_NOT_ALLOWED_ON_REPLY 예외")
    void createComment_offerOnReply() {
        given(productService.findProductById(100L)).willReturn(product);
        given(commentRepository.findById(500L)).willReturn(Optional.of(rootComment));
        given(userService.findUserById(20L)).willReturn(buyer);

        CommentDto.CreateRequest request = new CommentDto.CreateRequest();
        ReflectionTestUtils.setField(request, "parentCommentId", 500L);
        ReflectionTestUtils.setField(request, "content", "대댓글");
        ReflectionTestUtils.setField(request, "offerPrice", 7000);

        assertThatThrownBy(() -> commentService.createComment(1L, 20L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OFFER_NOT_ALLOWED_ON_REPLY);
    }

    @Test
    @DisplayName("6-2. 판매자가 가격 제안 시 OFFER_NOT_ALLOWED_FOR_SELLER 예외")
    void createComment_sellerOfferPrice() {
        given(productService.findProductById(100L)).willReturn(product);
        given(userService.findUserById(10L)).willReturn(seller);

        CommentDto.CreateRequest request = new CommentDto.CreateRequest();
        ReflectionTestUtils.setField(request, "content", "판매자의 댓글");
        ReflectionTestUtils.setField(request, "offerPrice", 9000);

        assertThatThrownBy(() -> commentService.createComment(1L, 10L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OFFER_NOT_ALLOWED_FOR_SELLER);
    }

    @Test
    @DisplayName("6-3. 타인의 댓글 삭제 시도 시 FORBIDDEN 예외")
    void deleteComment_notAuthor() {
        given(commentRepository.findById(500L)).willReturn(Optional.of(rootComment));

        assertThatThrownBy(() -> commentService.deleteComment(999L, 500L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("6-3. 대댓글이 있는 댓글 삭제 시 소프트 딜리트 처리")
    void deleteComment_withChildren_softDelete() {
        Comment child = Comment.builder().product(product).user(seller).parent(rootComment).content("답글").build();
        rootComment.getChildren().add(child);

        given(commentRepository.findById(500L)).willReturn(Optional.of(rootComment));

        commentService.deleteComment(20L, 500L);

        assertThat(rootComment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("6-3. 대댓글이 없는 댓글 삭제 시 하드 딜리트 처리")
    void deleteComment_noChildren_hardDelete() {
        given(commentRepository.findById(500L)).willReturn(Optional.of(rootComment));

        commentService.deleteComment(20L, 500L);

        verify(commentRepository).delete(rootComment);
    }

    @Test
    @DisplayName("6-4. 좋아요 토글 - 등록 및 해제")
    void toggleLike() {
        given(commentRepository.findById(500L)).willReturn(Optional.of(rootComment));
        given(userService.findUserById(20L)).willReturn(buyer);
        given(commentLikeRepository.existsByCommentIdAndUserId(500L, 20L)).willReturn(false);

        CommentDto.LikeResponse response = commentService.toggleLike(20L, 500L);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.getLikeCount()).isEqualTo(1);
        verify(commentLikeRepository).save(any(CommentLike.class));
    }
}
