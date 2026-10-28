package com.alumni.management.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.alumni.management.security.FileUploadSecurityUtil;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = FileUploadSecurityUtil.getUploadRoot();
        Path uploadsDir = uploadRoot.resolve("uploads").normalize();
        String uploadRootUri = uploadsDir.toUri().toString();
        if (!uploadRootUri.endsWith("/")) {
            uploadRootUri += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadRootUri, "file:uploads/", "file:./uploads/")
                .setCachePeriod(3600);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
