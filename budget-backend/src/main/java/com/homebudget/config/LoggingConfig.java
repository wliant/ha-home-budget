package com.homebudget.config;

import com.homebudget.filter.CorrelationIdFilter;
import com.homebudget.interceptor.LoggingInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for structured logging infrastructure.
 *
 * This class configures:
 * - AOP support for performance logging aspects
 * - Filter registration for correlation ID and user context
 * - Interceptor registration for request/response logging
 */
@Configuration
@EnableAspectJAutoProxy
public class LoggingConfig implements WebMvcConfigurer {

    /**
     * Register CorrelationIdFilter to run before all other filters.
     * This ensures correlation ID and user context are available for all logging.
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CorrelationIdFilter());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Register LoggingInterceptor for request/response logging.
     * Excludes actuator endpoints to avoid cluttering logs with health checks.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**");
    }
}
