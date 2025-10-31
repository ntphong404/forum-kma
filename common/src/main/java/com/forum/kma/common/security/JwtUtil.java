package com.forum.kma.common.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties props;

    public String generateAccessToken(String userId, String roleId, String sessionId) {
        return generateToken(userId, roleId, sessionId, props.getAccessExpirationMs(), "access");
    }

    public String generateRefreshToken(String userId, String roleId, String sessionId) {
        return generateToken(userId, roleId, sessionId, props.getRefreshExpirationMs(), "refresh");
    }

    private String generateToken(String userId, String roleId, String sessionId, long validityMs, String type) {
        try {
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userId)
                    .issuer(props.getIssuer())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + validityMs))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("type", type);

            if (roleId != null) claimsBuilder.claim("roleId", roleId);

            if (sessionId != null) claimsBuilder.claim("sid", sessionId);

            JWTClaimsSet claims = claimsBuilder.build();

            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new MACSigner(props.getSecret().getBytes()));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Cannot generate JWT", e);
        }
    }

    public JwtClaims validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(props.getSecret().getBytes());

            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("JWT signature invalid");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date())) {
                throw new RuntimeException("JWT expired");
            }

            return new JwtClaims(
                    claims.getSubject(),
                    claims.getStringClaim("roleId"),
                    claims.getStringClaim("sid"),
                    claims.getStringClaim("type")
            );

        } catch (JOSEException | ParseException e) {
            throw new RuntimeException("Invalid JWT", e);
        }
    }
}
