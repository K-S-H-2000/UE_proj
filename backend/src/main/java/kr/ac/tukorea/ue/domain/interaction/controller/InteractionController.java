package kr.ac.tukorea.ue.domain.interaction.controller;

import kr.ac.tukorea.ue.domain.interaction.service.BlockService;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import kr.ac.tukorea.ue.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class InteractionController {

    private final BlockService blockService;

    /**
     * 9-1. 차단 등록 / 해제 (토글)
     */
    @PostMapping("/{userId}/block")
    public ResponseEntity<Void> blockUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId
    ) {
        blockService.toggleBlock(userPrincipal.getUserId(), userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/block")
    public ResponseEntity<Void> unblockUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId
    ) {
        blockService.toggleBlock(userPrincipal.getUserId(), userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 9-2. 차단 목록 조회
     */
    @GetMapping("/me/blocks")
    public ResponseEntity<List<UserDto.SimpleUserResponse>> getMyBlocks(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<UserDto.SimpleUserResponse> response = blockService.getMyBlocks(userPrincipal.getUserId());
        return ResponseEntity.ok(response);
    }
}
