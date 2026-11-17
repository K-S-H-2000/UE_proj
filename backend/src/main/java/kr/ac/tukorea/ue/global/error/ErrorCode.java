package kr.ac.tukorea.ue.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // === 공통 ===
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않아요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요해요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없어요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없어요."),
    CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌해요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했어요."),

    // === 인증 (AUTH) ===
    DOMAIN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "지원하지 않는 이메일 도메인이에요."),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일이에요."),
    CODE_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않아요."),
    CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료됐어요."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임이에요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰이에요."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료됐어요."),

    // === 상품 (PRODUCT) ===
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지는 최대 10장까지 업로드할 수 있어요."),
    CANNOT_DELETE_RESERVED_PRODUCT(HttpStatus.CONFLICT, "예약 진행 중인 상품은 삭제할 수 없어요. 예약을 먼저 취소해 주세요."),
    CANNOT_DELETE_SOLD_PRODUCT(HttpStatus.CONFLICT, "판매완료된 상품은 삭제할 수 없어요."),
    PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "판매중 상태가 아닌 상품이에요."),
    CANNOT_REQUEST_OWN_PRODUCT(HttpStatus.BAD_REQUEST, "본인 상품에는 구매 요청을 할 수 없어요."),

    // === 댓글 (COMMENT) ===
    REPLY_DEPTH_EXCEEDED(HttpStatus.CONFLICT, "대댓글에는 답글을 달 수 없어요."),
    OFFER_NOT_ALLOWED_ON_REPLY(HttpStatus.BAD_REQUEST, "대댓글에는 가격 제안을 할 수 없어요."),
    OFFER_NOT_ALLOWED_FOR_SELLER(HttpStatus.BAD_REQUEST, "판매자는 가격 제안을 할 수 없어요."),

    // === 거래 (TRANSACTION) ===
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "현재 상태에서 요청한 전이를 수행할 수 없어요."),
    ACTIVE_TRANSACTION_EXISTS(HttpStatus.CONFLICT, "이미 진행 중인 거래 요청이 있어요."),

    // === 차단 (BLOCK) ===
    BLOCKED_RELATION(HttpStatus.FORBIDDEN, "차단 관계라 요청을 수행할 수 없어요."),
    CANNOT_BLOCK_ACTIVE_TRANSACTION(HttpStatus.CONFLICT, "진행 중인 거래가 있는 사용자는 차단할 수 없어요.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
