package kr.ac.tukorea.ue.domain.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        log.info("==================================================");
        log.info("[Email Verification] To: {}, Code: [{}]", toEmail, code);
        log.info("==================================================");
    }
}
