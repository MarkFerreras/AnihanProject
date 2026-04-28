package com.example.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Root → index
        registry.addViewController("/").setViewName("forward:/index.html");

        // Clean URL → static file (internal forward, address bar stays clean)
        registry.addViewController("/index").setViewName("forward:/index.html");
        registry.addViewController("/admin").setViewName("forward:/admin.html");
        registry.addViewController("/registrar").setViewName("forward:/registrar.html");
        registry.addViewController("/trainer").setViewName("forward:/trainer.html");
        registry.addViewController("/logs").setViewName("forward:/logs.html");
        registry.addViewController("/subjects").setViewName("forward:/subjects.html");
        registry.addViewController("/student-records").setViewName("forward:/student-records.html");
        registry.addViewController("/add-user").setViewName("forward:/add-user.html");
        registry.addViewController("/edit-user").setViewName("forward:/edit-user.html");

        // Legacy .html redirects are handled by HtmlRedirectFilter to avoid
        // a redirect loop: addRedirectViewController intercepts internal forwards too.
    }
}
