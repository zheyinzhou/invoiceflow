package com.zzy.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private static String toOrigin(String base) {
        try {
            var u = java.net.URI.create(base);
            String port = (u.getPort() == -1) ? "" : (":" + u.getPort());
            return u.getScheme() + "://" + u.getHost() + port;
        } catch (Exception e) {
            return base.replaceAll("/+$", "");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String origin = toOrigin(frontendBaseUrl); // -> http://localhost:5173
        registry.addMapping("/api/**")
                .allowedOrigins(origin)
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

