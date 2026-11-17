package kr.ac.tukorea.ue.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import kr.ac.tukorea.ue.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, Long universityId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("universityId", universityId)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String createSignupToken(String email, Long universityId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getSignupTokenExpiration());

        return Jwts.builder()
                .subject(email)
                .claim("universityId", universityId)
                .claim("type", "signup")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public Long getUniversityId(String token) {
        Claims claims = parseClaims(token);
        return claims.get("universityId", Long.class);
    }

    public String getSignupEmail(String signupToken) {
        Claims claims = parseClaims(signupToken);
        if (!"signup".equals(claims.get("type"))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return claims.getSubject();
    }

    public Long getSignupUniversityId(String signupToken) {
        Claims claims = parseClaims(signupToken);
        if (!"signup".equals(claims.get("type"))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return claims.get("universityId", Long.class);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        Long userId = Long.valueOf(claims.getSubject());
        Long universityId = claims.get("universityId", Long.class);

        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .universityId(universityId)
                .role("ROLE_USER")
                .build();

        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }
}
