package com.zzy.web;

import com.zzy.auth.QboAuthService;
import com.zzy.auth.TokenStore;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AuthController {

    private final QboAuthService auth;
    private final TokenStore store;

    public AuthController(QboAuthService auth, TokenStore store) {
        this.auth = auth;
        this.store = store;
    }

    @GetMapping("/connect")
    public RedirectView connect(HttpSession session) {
        String csrf = auth.generateCsrf();
        session.setAttribute("csrfToken", csrf);
        return new RedirectView(auth.buildAuthUrl(csrf), true, true, false);
    }

    @GetMapping("/oauth2redirect")
    public RedirectView callback(@RequestParam("code") String code,
                                 @RequestParam(value="realmId", required=false) String realmId,
                                 @RequestParam("state") String state,
                                 HttpSession session) throws Exception {
        // 1) CSRF
        Object saved = session.getAttribute("csrfToken");
        if (saved == null || !saved.equals(state)) {
            throw new IllegalStateException("Invalid state (CSRF check failed)");
        }

        // 2) realmId
        if (realmId == null || realmId.isBlank()) {
            throw new IllegalStateException("Missing realmId from OAuth callback");
        }

        // 3) code exchange token
        auth.exchangeCodeForTokens(code);

        // 4) save realmId
        store.setRealmId(realmId);

        return new RedirectView("http://localhost:5173/");
    }

    @GetMapping("/connected") @ResponseBody
    public Object connected() {
        return java.util.Map.of("status", store.hasTokens() ? "connected" : "not_connected",
                "realmId", store.getRealmId());
    }
}
