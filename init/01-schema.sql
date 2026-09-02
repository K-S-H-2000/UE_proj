-- =====================================================================
-- 유이 (UE: University Ecosystem) - 데이터베이스 스키마
-- PostgreSQL 16
-- 작성: 김승현 / 기준 문서: 시스템 설계서 2장, 요구사항 명세서 v2.8
-- =====================================================================
-- 실행: psql -U ue_admin -d ue -f schema.sql
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. 대학 (멀티테넌트 기준)
-- ---------------------------------------------------------------------
CREATE TABLE universities (
    university_id   BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    email_domain    VARCHAR(100)    NOT NULL UNIQUE,   -- 화이트리스트 (예: tukorea.ac.kr)
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE universities IS '서비스 대상 대학과 공식 이메일 도메인';

-- ---------------------------------------------------------------------
-- 2. 사용자
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id           BIGSERIAL     PRIMARY KEY,
    university_id     BIGINT        NOT NULL REFERENCES universities(university_id),
    email             VARCHAR(255)  NOT NULL UNIQUE,   -- 학교 이메일, 변경 불가
    nickname          VARCHAR(30)   NOT NULL,
    department        VARCHAR(50),                     -- 본인·통계용, 타인 비공개
    profile_image_url VARCHAR(500),
    sale_count        INTEGER       NOT NULL DEFAULT 0 CHECK (sale_count  >= 0),
    buy_count         INTEGER       NOT NULL DEFAULT 0 CHECK (buy_count   >= 0),
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE','RESTRICTED')),
    verified_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),  -- 재인증 기준 시각
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    -- AUTH-03a: 같은 학교 안에서 닉네임 유일 (동시 가입 경쟁 조건까지 차단)
    CONSTRAINT uq_users_univ_nickname UNIQUE (university_id, nickname)
);
CREATE INDEX idx_users_university ON users(university_id);
CREATE INDEX idx_users_verified   ON users(verified_at);   -- 재인증 배치용
COMMENT ON COLUMN users.sale_count IS 'transactions 집계값의 비정규화 (주 1회 정합성 검증)';

-- ---------------------------------------------------------------------
-- 3. 상품
-- ---------------------------------------------------------------------
CREATE TABLE products (
    product_id    BIGSERIAL     PRIMARY KEY,
    university_id BIGINT        NOT NULL REFERENCES universities(university_id),
    seller_id     BIGINT        NOT NULL REFERENCES users(user_id),
    title         VARCHAR(100)  NOT NULL,
    department    VARCHAR(50)   NOT NULL,   -- 학과 카테고리 또는 '학과 무관'
    price         INTEGER       NOT NULL CHECK (price >= 0),
    condition     VARCHAR(20)   NOT NULL CHECK (condition IN ('UNOPENED','OPENED')),
    description   TEXT,
    status        VARCHAR(20)   NOT NULL DEFAULT 'ON_SALE'
                  CHECK (status IN ('ON_SALE','RESERVE_REQUESTED','RESERVED','SOLD')),
    favorite_count INTEGER      NOT NULL DEFAULT 0 CHECK (favorite_count >= 0),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
-- 피드: 학교 + 최신순 / 학과 필터 / 상태 필터
CREATE INDEX idx_products_feed   ON products(university_id, created_at DESC);
CREATE INDEX idx_products_dept   ON products(university_id, department, status);
CREATE INDEX idx_products_seller ON products(seller_id);
COMMENT ON COLUMN products.status IS '화면 표시용 현재 상태. 이력은 transactions가 보관';

-- ---------------------------------------------------------------------
-- 4. 상품 이미지 (상품당 최대 10장 - 애플리케이션에서 검증)
-- ---------------------------------------------------------------------
CREATE TABLE product_images (
    image_id   BIGSERIAL    PRIMARY KEY,
    product_id BIGINT       NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    s3_url     VARCHAR(500) NOT NULL,
    sort_order SMALLINT     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_product_images ON product_images(product_id, sort_order);

-- ---------------------------------------------------------------------
-- 5. 댓글 / 대댓글 (2단계 제한, 가격 제안은 최상위만)
-- ---------------------------------------------------------------------
CREATE TABLE comments (
    comment_id        BIGSERIAL   PRIMARY KEY,
    product_id        BIGINT      NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    user_id           BIGINT      NOT NULL REFERENCES users(user_id),
    parent_comment_id BIGINT      REFERENCES comments(comment_id) ON DELETE CASCADE,
    content           TEXT        NOT NULL,
    offer_price       INTEGER     CHECK (offer_price IS NULL OR offer_price >= 0),
    like_count        INTEGER     NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ,
    -- CMT-08: 대댓글에는 가격 제안 불가
    CONSTRAINT ck_offer_only_root CHECK (parent_comment_id IS NULL OR offer_price IS NULL)
);
CREATE INDEX idx_comments_product ON comments(product_id, created_at);
CREATE INDEX idx_comments_parent  ON comments(parent_comment_id);
CREATE INDEX idx_comments_user    ON comments(user_id);

-- CMT-08: 대댓글의 대댓글 금지 (깊이 2단계 제한)
CREATE OR REPLACE FUNCTION fn_check_comment_depth() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_comment_id IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM comments
                   WHERE comment_id = NEW.parent_comment_id
                     AND parent_comment_id IS NOT NULL) THEN
            RAISE EXCEPTION '대댓글에는 답글을 달 수 없습니다 (2단계 제한)';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comment_depth
    BEFORE INSERT OR UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION fn_check_comment_depth();

-- ---------------------------------------------------------------------
-- 6. 댓글 좋아요 / 답글 알림  (단일 PK + UNIQUE : 모든 관계 비식별 통일)
-- ---------------------------------------------------------------------
CREATE TABLE comment_likes (
    like_id    BIGSERIAL   PRIMARY KEY,
    comment_id BIGINT      NOT NULL REFERENCES comments(comment_id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_comment_like UNIQUE (comment_id, user_id)
);

CREATE TABLE comment_notifications (
    notification_id BIGSERIAL   PRIMARY KEY,
    comment_id      BIGINT      NOT NULL REFERENCES comments(comment_id) ON DELETE CASCADE,
    user_id         BIGINT      NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_comment_notification UNIQUE (comment_id, user_id)
);

-- ---------------------------------------------------------------------
-- 7. 채팅방 (상품 기준 · 상대별 1개)
-- ---------------------------------------------------------------------
CREATE TABLE chat_rooms (
    room_id             BIGSERIAL   PRIMARY KEY,
    product_id          BIGINT      NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    buyer_id            BIGINT      NOT NULL REFERENCES users(user_id),
    seller_id           BIGINT      NOT NULL REFERENCES users(user_id),  -- 목록 조회 최적화용
    created_via         VARCHAR(20) NOT NULL
                        CHECK (created_via IN ('PURCHASE_REQUEST','TRADE_REQUEST')),
    buyer_last_read_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),   -- 안 읽은 개수 계산용
    seller_last_read_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_message_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),   -- 목록 정렬용
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- CHAT-02: 같은 상품에 대해 상대별 채팅방 1개
    CONSTRAINT uq_chat_room UNIQUE (product_id, buyer_id),
    CONSTRAINT ck_chat_not_self CHECK (buyer_id <> seller_id)
);
CREATE INDEX idx_chat_buyer  ON chat_rooms(buyer_id,  last_message_at DESC);
CREATE INDEX idx_chat_seller ON chat_rooms(seller_id, last_message_at DESC);

-- ---------------------------------------------------------------------
-- 8. 메시지
-- ---------------------------------------------------------------------
CREATE TABLE messages (
    message_id   BIGSERIAL   PRIMARY KEY,
    room_id      BIGINT      NOT NULL REFERENCES chat_rooms(room_id) ON DELETE CASCADE,
    sender_id    BIGINT      NOT NULL REFERENCES users(user_id),
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT'
                 CHECK (message_type IN ('TEXT','IMAGE','SYSTEM')),
    content      TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_messages_room ON messages(room_id, created_at);

-- ---------------------------------------------------------------------
-- 9. 거래 (예약 요청 ~ 완료 이력)
-- ---------------------------------------------------------------------
CREATE TABLE transactions (
    transaction_id     BIGSERIAL   PRIMARY KEY,
    product_id         BIGINT      NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    seller_id          BIGINT      NOT NULL REFERENCES users(user_id),
    buyer_id           BIGINT      NOT NULL REFERENCES users(user_id),
    status             VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
                       CHECK (status IN ('REQUESTED','RESERVED','CANCELLED','COMPLETED')),
    requested_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    accepted_at        TIMESTAMPTZ,
    cancelled_at       TIMESTAMPTZ,
    cancelled_by       BIGINT      REFERENCES users(user_id),
    cancel_reason      VARCHAR(200),
    completed_at       TIMESTAMPTZ,
    buyer_confirmed_at TIMESTAMPTZ,          -- TRST-04: 이 값이 있어야 완료 횟수 반영
    CONSTRAINT ck_trx_not_self CHECK (seller_id <> buyer_id)
);
-- TRST-01c: 한 상품에 동시에 유효한 예약 요청·예약은 1건
CREATE UNIQUE INDEX uq_active_transaction
    ON transactions(product_id)
    WHERE status IN ('REQUESTED','RESERVED');

CREATE INDEX idx_trx_product ON transactions(product_id);
CREATE INDEX idx_trx_seller  ON transactions(seller_id, status);
CREATE INDEX idx_trx_buyer   ON transactions(buyer_id,  status);

-- ---------------------------------------------------------------------
-- 10. 찜 / 차단
-- ---------------------------------------------------------------------
CREATE TABLE favorites (
    favorite_id BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    product_id  BIGINT      NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_favorite UNIQUE (user_id, product_id)
);
CREATE INDEX idx_favorites_user ON favorites(user_id, created_at DESC);

CREATE TABLE blocks (
    block_id   BIGSERIAL   PRIMARY KEY,
    blocker_id BIGINT      NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    blocked_id BIGINT      NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_block UNIQUE (blocker_id, blocked_id),
    CONSTRAINT ck_block_not_self CHECK (blocker_id <> blocked_id)
);
-- BLK-02: 조회 시 양방향 필터를 위해 두 방향 모두 인덱스
CREATE INDEX idx_blocks_blocker ON blocks(blocker_id);
CREATE INDEX idx_blocks_blocked ON blocks(blocked_id);

-- ---------------------------------------------------------------------
-- 11. 신고
-- ---------------------------------------------------------------------
CREATE TABLE reports (
    report_id         BIGSERIAL   PRIMARY KEY,
    reporter_id       BIGINT      NOT NULL REFERENCES users(user_id),
    -- 신고 대상: 상품 / 댓글 / 사용자 중 정확히 하나만 채운다 (각각 외래키로 무결성 보장)
    target_product_id BIGINT      REFERENCES products(product_id) ON DELETE CASCADE,
    target_comment_id BIGINT      REFERENCES comments(comment_id) ON DELETE CASCADE,
    target_user_id    BIGINT      REFERENCES users(user_id)       ON DELETE CASCADE,
    reason_code       VARCHAR(30) NOT NULL
                      CHECK (reason_code IN ('NO_SHOW','MISMATCH','PROHIBITED','RUDE','ETC')),
    detail            TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'RECEIVED'
                      CHECK (status IN ('RECEIVED','RESOLVED')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at       TIMESTAMPTZ,
    admin_note        TEXT,
    -- RPT-01: 신고 대상은 반드시 하나여야 한다
    CONSTRAINT ck_report_single_target
        CHECK (num_nonnulls(target_product_id, target_comment_id, target_user_id) = 1),
    -- 자기 자신은 신고할 수 없다
    CONSTRAINT ck_report_not_self
        CHECK (target_user_id IS NULL OR target_user_id <> reporter_id)
);
CREATE INDEX idx_reports_status  ON reports(status, created_at DESC);
CREATE INDEX idx_reports_product ON reports(target_product_id);
CREATE INDEX idx_reports_comment ON reports(target_comment_id);
CREATE INDEX idx_reports_user    ON reports(target_user_id);

-- ---------------------------------------------------------------------
-- 12. 검색 동의어 (SRCH-02)
-- ---------------------------------------------------------------------
CREATE TABLE synonyms (
    synonym_id BIGSERIAL   PRIMARY KEY,
    keyword    VARCHAR(50) NOT NULL,
    synonym    VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_synonym UNIQUE (keyword, synonym)
);
CREATE INDEX idx_synonyms_keyword ON synonyms(keyword);

-- ---------------------------------------------------------------------
-- 13. 행동 로그 (BAT-02) - 배치 집계 대상
-- ---------------------------------------------------------------------
CREATE TABLE activity_logs (
    log_id        BIGSERIAL   PRIMARY KEY,
    user_id       BIGINT      REFERENCES users(user_id) ON DELETE SET NULL,
    university_id BIGINT      REFERENCES universities(university_id),
    event_type    VARCHAR(30) NOT NULL,   -- SEARCH / VIEW / FAVORITE / COMMENT / RESERVE / COMPLETE ...
    product_id    BIGINT,
    keyword       VARCHAR(100),           -- 결과 0건 검색어 분석용
    result_count  INTEGER,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_logs_created ON activity_logs(created_at);
CREATE INDEX idx_logs_event   ON activity_logs(event_type, created_at);

-- ---------------------------------------------------------------------
-- 14. 일별 집계 (NFR-07) - Airflow 산출 결과
-- ---------------------------------------------------------------------
CREATE TABLE kpi_daily (
    stat_date        DATE   NOT NULL,
    university_id    BIGINT NOT NULL REFERENCES universities(university_id),
    signup_count     INTEGER NOT NULL DEFAULT 0,
    active_users     INTEGER NOT NULL DEFAULT 0,
    product_count    INTEGER NOT NULL DEFAULT 0,
    reserved_count   INTEGER NOT NULL DEFAULT 0,
    completed_count  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (stat_date, university_id)
);

COMMIT;
