package com.flowbase.engine.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfiguration {
    @Value("${flowbase.jwt.secret}")
    private String secret;
    @Value("${flowbase.jwt.expriationminutes}")
    private long expirationMinutes;
    @Bean
    public SecretKey jwtSecretKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public long getExpriationMinutes(){
        return this.expirationMinutes;
    }
}
