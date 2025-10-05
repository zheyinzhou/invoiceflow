package com.zzy.auth;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class TokenStore {
    private String accessToken;
    private String refreshToken;
    private long   accessTokenExpiryEpochSec;
    private String realmId;
    public boolean hasTokens() { return accessToken != null && refreshToken != null; }
}
