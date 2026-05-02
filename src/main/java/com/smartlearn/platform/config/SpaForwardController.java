package com.smartlearn.platform.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Forwards non-API, non-resource routes to index.html for Vue Router SPA.
 */
@Component
public class SpaForwardController implements WebMvcConfigurer {

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new SpaFallbackInterceptor())
            .addPathPatterns("/**")
            .excludePathPatterns("/api/**", "/uploads/**", "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/");
    }

    private static class SpaFallbackInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String path = request.getRequestURI();
            // Forward SPA routes (not API, not static assets) to index.html
            if (!path.startsWith("/api/") && !path.startsWith("/uploads/") &&
                !path.equals("/error") && !path.contains(".")) {
                request.getRequestDispatcher("/index.html").forward(request, response);
                return false;
            }
            return true;
        }
    }
}
