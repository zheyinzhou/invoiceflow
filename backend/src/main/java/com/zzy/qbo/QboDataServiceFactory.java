package com.zzy.qbo;

import com.zzy.auth.QboAuthService;
import com.zzy.auth.TokenStore;
import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import org.springframework.stereotype.Component;
import com.intuit.ipp.util.Config;

@Component
public class QboDataServiceFactory {
    private final TokenStore store;
    private final QboAuthService auth;

    public QboDataServiceFactory(TokenStore store, QboAuthService auth) {
        this.store = store; this.auth = auth;
    }

    public DataService get() throws Exception {
        String url = "https://sandbox-quickbooks.api.intuit.com" + "/v3/company";  // TODO: tobe configuration
        Config.setProperty(Config.BASE_URL_QBO, url);
        auth.refreshIfNeeded();
        var ctx = new Context(new OAuth2Authorizer(store.getAccessToken()), ServiceType.QBO, store.getRealmId());
        return new DataService(ctx);
    }
}
