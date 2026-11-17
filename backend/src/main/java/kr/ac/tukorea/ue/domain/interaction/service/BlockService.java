package kr.ac.tukorea.ue.domain.interaction.service;

import kr.ac.tukorea.ue.domain.interaction.entity.Block;
import kr.ac.tukorea.ue.domain.interaction.repository.BlockRepository;
import kr.ac.tukorea.ue.domain.transaction.entity.TransactionStatus;
import kr.ac.tukorea.ue.domain.transaction.repository.TransactionRepository;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final BlockRepository blockRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    /**
     * 9-1. 차단 등록 / 해제 (토글)
     */
    @Transactional
    public boolean toggleBlock(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "자기 자신을 차단할 수 없어요.");
        }

        // BLK-03: 진행 중인 거래가 있는 사용자는 차단 불가
        List<TransactionStatus> activeStatuses = List.of(TransactionStatus.REQUESTED, TransactionStatus.RESERVED);
        boolean hasActiveTrx = transactionRepository.existsBySellerIdAndBuyerIdAndStatusIn(blockerId, blockedId, activeStatuses)
                || transactionRepository.existsBySellerIdAndBuyerIdAndStatusIn(blockedId, blockerId, activeStatuses);

        if (hasActiveTrx) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK_ACTIVE_TRANSACTION);
        }

        boolean alreadyBlocked = blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);

        if (alreadyBlocked) {
            blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
            return false;
        } else {
            User blocker = userService.findUserById(blockerId);
            User blocked = userService.findUserById(blockedId);
            Block block = Block.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .build();
            blockRepository.save(block);
            return true;
        }
    }

    /**
     * 9-2. 차단 목록 조회
     */
    public List<UserDto.SimpleUserResponse> getMyBlocks(Long userId) {
        List<Block> blocks = blockRepository.findByBlockerIdOrderByCreatedAtDesc(userId);
        return blocks.stream()
                .map(b -> UserDto.SimpleUserResponse.from(b.getBlocked()))
                .toList();
    }
}
