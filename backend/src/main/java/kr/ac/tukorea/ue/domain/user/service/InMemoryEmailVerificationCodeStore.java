package kr.ac.tukorea.ue.domain.user.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryEmailVerificationCodeStore implements EmailVerificationCodeStore {

    private static class VerificationEntry {
        private final String code;
        private final Instant expiresAt;

        public VerificationEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        public boolean isValid(String inputCode) {
            return this.code.equals(inputCode) && Instant.now().isBefore(this.expiresAt);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(this.expiresAt);
        }
    }

    private final Map<String, VerificationEntry> store = new ConcurrentHashMap<>();

    @Override
    public void save(String email, String code, long durationSeconds) {
        Instant expiresAt = Instant.now().plusSeconds(durationSeconds);
        store.put(email, new VerificationEntry(code, expiresAt));
    }

    @Override
    public boolean verify(String email, String code) {
        VerificationEntry entry = store.get(email);
        if (entry == null) {
            return false;
        }
        return entry.isValid(code);
    }

    @Override
    public void remove(String email) {
        store.remove(email);
    }
}
