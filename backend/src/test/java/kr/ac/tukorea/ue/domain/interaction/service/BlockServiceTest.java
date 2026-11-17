package kr.ac.tukorea.ue.domain.interaction.service;

import kr.ac.tukorea.ue.domain.interaction.entity.Block;
import kr.ac.tukorea.ue.domain.interaction.repository.BlockRepository;
import kr.ac.tukorea.ue.domain.transaction.repository.TransactionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BlockService blockService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = User.builder().nickname("유저A").email("a@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(userA, "id", 1L);

        userB = User.builder().nickname("유저B").email("b@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(userB, "id", 2L);
    }

    @Test
    @DisplayName("9-1. 자기 자신 차단 시도 시 INVALID_INPUT 예외")
    void toggleBlock_selfBlock() {
        assertThatThrownBy(() -> blockService.toggleBlock(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("9-1. 진행 중인 거래가 있는 상대 차단 시 CANNOT_BLOCK_ACTIVE_TRANSACTION 예외 (BLK-03)")
    void toggleBlock_activeTransaction() {
        given(transactionRepository.existsBySellerIdAndBuyerIdAndStatusIn(any(), any(), any())).willReturn(true);

        assertThatThrownBy(() -> blockService.toggleBlock(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_BLOCK_ACTIVE_TRANSACTION);
    }

    @Test
    @DisplayName("9-1. 미차단 상태에서 토글 시 차단 등록 (true 반환)")
    void toggleBlock_register() {
        given(transactionRepository.existsBySellerIdAndBuyerIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(false);
        given(userService.findUserById(1L)).willReturn(userA);
        given(userService.findUserById(2L)).willReturn(userB);

        boolean result = blockService.toggleBlock(1L, 2L);

        assertThat(result).isTrue();
        verify(blockRepository).save(any(Block.class));
    }

    @Test
    @DisplayName("9-1. 이미 차단된 상태에서 토글 시 차단 해제 (false 반환)")
    void toggleBlock_unregister() {
        given(transactionRepository.existsBySellerIdAndBuyerIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(true);

        boolean result = blockService.toggleBlock(1L, 2L);

        assertThat(result).isFalse();
        verify(blockRepository).deleteByBlockerIdAndBlockedId(1L, 2L);
    }
}
