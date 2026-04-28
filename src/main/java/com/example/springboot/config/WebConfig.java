package com.example.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Root → /index
        registry.addViewController("/").setViewName("forward:/index.html");

        // Clean URL forwards
        registry.addViewController("/index").setViewName("forward:/index.html");
        registry.addViewController("/admin").setViewName("forward:/admin.html");
        registry.addViewController("/registrar").setViewName("forward:/registrar.html");
        registry.addViewController("/trainer").setViewName("forward:/trainer.html");
        registry.addViewController("/logs").setViewName("forward:/logs.html");
        registry.addViewController("/subjects").setViewName("forward:/subjects.html");
        registry.addViewController("/student-records").setViewName("forward:/student-records.html");
        registry.addViewController("/add-user").setViewName("forward:/add-user.html");
        registry.addViewController("/edit-user").setViewName("forward:/edit-user.html");

        // Legacy .html → clean URL (301 permanent redirects)
        registry.addRedirectViewController("/index.html", "/index")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        registry.addRedirectViewController("/admin.html", "/admin")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        registry.addRedirectViewController("/registrar.html", "/registrar")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        registry.addRedirectViewController("/trainer.html", "/trainer")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        registry.addRedirectViewController("/logs.html", "/logs")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        registry.addRedirectViewController("/subjects.html", "/subjects")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        registry.addRedirectViewController("/student-records.html", "/student-records")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        registry.addRedirectViewController("/add-user.html", "/add-user")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        registry.addRedirectViewController("/edit-user.html", "/edit-user")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
    }
}
