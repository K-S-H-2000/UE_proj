package kr.ac.tukorea.ue.domain.user.service;

public interface EmailVerificationCodeStore {
    void save(String email, String code, long durationSeconds);
    boolean verify(String email, String code);
    void remove(String email);
}
