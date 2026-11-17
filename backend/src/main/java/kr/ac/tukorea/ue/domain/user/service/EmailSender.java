package kr.ac.tukorea.ue.domain.user.service;

public interface EmailSender {
    void sendVerificationCode(String toEmail, String code);
}
