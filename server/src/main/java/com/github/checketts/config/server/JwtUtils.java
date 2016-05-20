package com.github.checketts.config.server;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.sql.Date;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    public static final String APP_ENVIRONMENT = "appEnvironment";
    public static final String APP_SERVICE = "appService";
    public static final String APP_AUTHORITIES = "appAuthorities";
    private final static Logger LOG = LoggerFactory.getLogger(JwtUtils.class);
    private final List<MACVerifier> serviceAuthVerifiers;
    private SecureRandom secureRandom;

    @Autowired
    public JwtUtils(@Value("#{'${config.server.authentication.jwt.secrets}'.split(',')}")
                            List<String> appServiceAuthSecrets) {
        this.serviceAuthVerifiers = appServiceAuthSecrets.stream()
                .map(JwtUtils::newMacVerifier)
                .collect(Collectors.toList());
        this.secureRandom = new SecureRandom();
    }

    private static MACVerifier newMacVerifier(String v) {
        try {
            return new MACVerifier(v);
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public SignedJWT appServiceJwt(String serviceId, String environmentId) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .jwtID(String.valueOf(secureRandom.nextInt()))
                .claim(APP_SERVICE, serviceId)
                .claim(APP_ENVIRONMENT, environmentId)
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        sign(jwt);
        return jwt;
    }

    public SignedJWT adminJwt(String username) {
        return adminJwt(username, Instant.now().plus(1, ChronoUnit.HOURS));
    }

    public SignedJWT adminJwt(String username, Instant instant) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .jwtID(String.valueOf(secureRandom.nextInt()))
                .subject(username)
                .claim(APP_AUTHORITIES, "Admin")
                .expirationTime(Date.from(instant))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        sign(jwt);
        return jwt;
    }

    private void sign(SignedJWT jwt) {
        try {
            MACSigner signer = new MACSigner(serviceAuthVerifiers.get(0).getSecret());
            jwt.sign(signer);
        } catch (JOSEException e) {
            String claimSet = "";
            try {
                claimSet = jwt.getJWTClaimsSet().toString();
            } catch (ParseException e1) {
                LOG.error("Unable to parse claimset", e1);
            }

            throw new IllegalStateException("Problem signing JWT token. claimSet=" + claimSet, e);
        }
    }

    public boolean isValidAppServiceJwt(SignedJWT jwt) throws ParseException {
        int secretCount = serviceAuthVerifiers.size();
        for (int i = 0; i < secretCount; i++) {
            JWTClaimsSet claimSet = jwt.getJWTClaimsSet();
            String serviceId = (String) claimSet.getClaim(APP_SERVICE);
            String environmentId = (String) claimSet.getClaim(APP_ENVIRONMENT);

            MACVerifier verifier = serviceAuthVerifiers.get(i);
            String secret = verifier.getSecretString();
            String secretPrefix = secret.substring(0, Math.min(3, secret.length()));
            String msgSuffix = String.format("secret=[%s... (%s of %s)], service=%s, env=%s, jwtId=%s",
                    secretPrefix, i + 1, secretCount, serviceId, environmentId, claimSet.getJWTID());
            try {
                boolean verified = jwt.verify(verifier);
                LOG.info("App service Jwt verified={}. {}", verified, msgSuffix);
                if (verified) {
                    return true;
                }
            } catch (JOSEException e) {
                throw new IllegalStateException(String.format("Problem verifying JWT token. %s", msgSuffix), e);
            }
        }
        return false;
    }
}
