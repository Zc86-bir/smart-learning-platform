package com.smartlearn.platform.config;

import com.smartlearn.platform.filter.JwtAuthenticationFilter;
import com.smartlearn.platform.interceptor.SecurityInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SecurityInterceptor securityInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public WebConfig(SecurityInterceptor securityInterceptor, RateLimitInterceptor rateLimitInterceptor, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.securityInterceptor = securityInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthenticationFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("X-Total-Count")
            .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**");
        registry.addInterceptor(securityInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward all non-API routes to Vue SPA
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
