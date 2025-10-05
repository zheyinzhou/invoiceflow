package com.zzy.auth;

import com.intuit.oauth2.config.Scope;
import com.intuit.oauth2.exception.InvalidRequestException;
import com.zzy.config.QboProps;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.data.BearerTokenResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QboAuthService {
    private final QboProps props;
    private final TokenStore store;

    private final OAuth2Config oauth2Config;
    private final OAuth2PlatformClient oauthClient;

    public QboAuthService(QboProps props, TokenStore store) {
        this.props = props;
        this.store = store;

        this.oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(
                props.getClientId(),
                props.getClientSecret())
                .callDiscoveryAPI(Environment.SANDBOX)
                .buildConfig();

        this.oauthClient = new OAuth2PlatformClient(oauth2Config);
    }

    public String buildAuthUrl(String state) {
        List<Scope> scopes = new ArrayList<Scope>();
        scopes.add(Scope.Accounting);
        try {
            return oauth2Config.prepareUrl(scopes, props.getRedirectUri(), state);
        } catch (InvalidRequestException e) {
            throw new RuntimeException(e);
        }
    }

    public void exchangeCodeForTokens(String code) throws Exception {
        BearerTokenResponse resp = oauthClient.retrieveBearerTokens(code, props.getRedirectUri());
        store.setAccessToken(resp.getAccessToken());
        store.setRefreshToken(resp.getRefreshToken());
        store.setAccessTokenExpiryEpochSec(System.currentTimeMillis()/1000 + resp.getExpiresIn());
    }

    public void refreshIfNeeded() throws Exception {
        long now = System.currentTimeMillis()/1000;
        if (store.getAccessToken() == null) throw new IllegalStateException("Not connected.");
        if (now + 30 < store.getAccessTokenExpiryEpochSec()) return;
        BearerTokenResponse r = oauthClient.refreshToken(store.getRefreshToken());
        store.setAccessToken(r.getAccessToken());
        store.setRefreshToken(r.getRefreshToken());
        store.setAccessTokenExpiryEpochSec(now + r.getExpiresIn());
    }

    public String generateCsrf() {
        return oauth2Config.generateCSRFToken();
    }
}
