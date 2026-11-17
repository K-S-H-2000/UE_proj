package kr.ac.tukorea.ue.domain.transaction.controller;

import jakarta.validation.Valid;
import kr.ac.tukorea.ue.domain.transaction.dto.TransactionDto;
import kr.ac.tukorea.ue.domain.transaction.service.TransactionService;
import kr.ac.tukorea.ue.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * 8-1. 예약 요청
     */
    @PostMapping("/chat/rooms/{roomId}/reservation")
    public ResponseEntity<TransactionDto.ReservationResponse> requestReservation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roomId
    ) {
        TransactionDto.ReservationResponse response = transactionService.requestReservation(
                userPrincipal.getUserId(), roomId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 8-2. 예약 수락
     */
    @PostMapping("/transactions/{transactionId}/accept")
    public ResponseEntity<Void> acceptReservation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long transactionId
    ) {
        transactionService.acceptReservation(userPrincipal.getUserId(), transactionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 8-2. 예약 거절
     */
    @PostMapping("/transactions/{transactionId}/reject")
    public ResponseEntity<Void> rejectReservation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long transactionId
    ) {
        transactionService.rejectReservation(userPrincipal.getUserId(), transactionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 8-3. 예약 요청 철회
     */
    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<Void> withdrawReservation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long transactionId
    ) {
        transactionService.withdrawReservation(userPrincipal.getUserId(), transactionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 8-4. 예약 취소
     */
    @PostMapping("/transactions/{transactionId}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long transactionId,
            @RequestBody(required = false) TransactionDto.CancelRequest request
    ) {
        String reason = (request != null) ? request.getReason() : null;
        transactionService.cancelReservation(userPrincipal.getUserId(), transactionId, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * 8-5. 판매완료 처리
     */
    @PostMapping("/transactions/{transactionId}/complete")
    public ResponseEntity<Void> completeTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long transactionId,
            @Valid @RequestBody(required = false) TransactionDto.CompleteRequest request
    ) {
        Integer agreedPrice = (request != null) ? request.getAgreedPrice() : null;
        transactionService.completeTransaction(userPrincipal.getUserId(), transactionId, agreedPrice);
        return ResponseEntity.ok().build();
    }

    /**
     * 8-6. 거래 완료 확인
     */
    @PostMapping("/transactions/{transactionId}/confirm")
    public ResponseEntity<Void> confirmTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionDto.ConfirmRequest request
    ) {
        transactionService.confirmTransaction(userPrincipal.getUserId(), transactionId, request.isConfirmed());
        return ResponseEntity.ok().build();
    }

    /**
     * 8-7. 거래 내역 조회
     */
    @GetMapping("/transactions/me")
    public ResponseEntity<List<TransactionDto.HistoryResponse>> getMyTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false, defaultValue = "SELLER") String role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        List<TransactionDto.HistoryResponse> response = transactionService.getMyTransactions(
                userPrincipal.getUserId(), role, pageable
        );
        return ResponseEntity.ok(response);
    }
}
