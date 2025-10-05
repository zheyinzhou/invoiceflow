package com.zzy.qbo;

import com.zzy.auth.QboAuthService;
import com.zzy.auth.TokenStore;
import com.zzy.config.QboProps;
import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.intuit.ipp.util.Config;

@Component
public class QboDataServiceFactory {
    private final TokenStore store;
    private final QboAuthService auth;
    private static final Logger log = LoggerFactory.getLogger(QboDataServiceFactory.class);

    public QboDataServiceFactory(TokenStore store, QboAuthService auth) {
        this.store = store; this.auth = auth;
    }

    public DataService get() throws Exception {
        String url = "https://sandbox-quickbooks.api.intuit.com" + "/v3/company";
        Config.setProperty(Config.BASE_URL_QBO, url);
//        Config.setProperty(Config.BASE_URL_QBO, "https://sandbox-quickbooks.api.intuit.com");
//        Config.setProperty(Config.TIMEOUT_REQUEST, "30000");            // 30s
//        Config.setProperty(Config.TIMEOUT_CONNECTION, "10000"); // 10s
        auth.refreshIfNeeded();
        var ctx = new Context(new OAuth2Authorizer(store.getAccessToken()), ServiceType.QBO, store.getRealmId());
//        log.info("QBO DIAG: realmId={}, token~={}",
//                store.getRealmId(), store.getAccessToken().substring(0,12)+"...");
//        log.info("QBO host should be sandbox-quickbooks.api.intuit.com when env=SANDBOX");
        return new DataService(ctx);
    }
}
