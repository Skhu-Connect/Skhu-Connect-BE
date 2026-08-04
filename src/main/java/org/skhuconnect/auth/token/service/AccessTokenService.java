package org.skhuconnect.auth.token.service;

import org.skhuconnect.user.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class AccessTokenService {

    public static final long EXPIRES_IN_SECONDS = 1800;
    private static final Duration VALIDITY = Duration.ofMinutes(30);

    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public AccessTokenService(JwtEncoder jwtEncoder, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    public String issue(User user) {
        Instant issuedAt = clock.instant();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(VALIDITY))
                .claim("role", "USER")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}