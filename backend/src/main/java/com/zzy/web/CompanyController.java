package com.zzy.web;

import com.intuit.ipp.util.Config;
import com.intuit.oauth2.config.Environment;
import com.zzy.auth.TokenStore;
import com.zzy.qbo.QboDataServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CompanyController {
    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final QboDataServiceFactory factory;
    private final TokenStore store;

    public CompanyController(QboDataServiceFactory factory, TokenStore store) {
        this.factory = factory; this.store = store;
    }

    @GetMapping("/company")
    public ResponseEntity<?> company() {
        if (!store.hasTokens()) {
            return ResponseEntity.status(401).body(Map.of(
                    "error","Not connected",
                    "hint","Hit /connect first"
            ));
        }

        // 1) 关键诊断打印（别打印完整 token）
        String realm = store.getRealmId();
        String env   = "SANDBOX"; // 如果你有 props.getEnvironment() 就用它
        String token = store.getAccessToken();
        String tokenPrefix = token == null ? "null" : token.substring(0, 8) + "...";
        log.info("QBO DIAG: realmId={}, env={}, accessToken~={}", realm, env, tokenPrefix);

        // 2) 解码 Access Token（不验证签名，只为看 claims：aud/scope/exp）
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String claimsJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                log.info("QBO DIAG: token.claims={}", claimsJson);
            }
        } catch (Exception ignore) { /* 解码失败不影响流程 */ }

        try {
            var ds = factory.get();
            var qr = ds.executeQuery("select * from companyinfo");
            return ResponseEntity.ok(qr);

        } catch (com.intuit.ipp.exception.InvalidTokenException e) {
            log.error("[InvalidToken] token invalid even after refresh. realmId={} msg={}", realm, e.getMessage(), e);
            return ResponseEntity.status(401).body(Map.of(
                    "error","invalid_token",
                    "message","Access token invalid or revoked. Reconnect."
            ));
        } catch (Exception e) {
            log.error("[Unexpected] realmId={} msg={}", realm, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error","unexpected","message", e.getMessage()));
        }
    }

}
