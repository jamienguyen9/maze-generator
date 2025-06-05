package com.mazegen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;


/**
 * Web configuration for the application
 * 
 * Configures:
 * - CORS (Cross-Origin Resource Sharing) for frontend integration
 * - Content negotiation for different response formats
 * - Static resource handling
 * - View resolvers for potential web UI
 * 
 * Enables the API to be consumed by web frontends 
 * running on different domains/ports during development
 */
@Configuration
@EnableWebMvc
 public class WebConfig implements WebMvcConfigurer{

    @Value("${maze.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${maze.cors.max-age:3600}")
    private long corsMaxAge;

    /**
     * Configure CORS mapping for all API endpoints
     * 
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(corsMaxAge);
        
        // health endpoints have separate mapping
        registry.addMapping("/actuator/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(corsMaxAge);
    }

    /**
     * Configure content negotiation to support multiple response formats
     * Clients can request JSON, XML, or plain text responses
     */
    @Override
    public void configureContentNegotiation(@NonNull ContentNegotiationConfigurer configurer) {
        configurer.favorParameter(true)
                  .parameterName("format")
                  .ignoreAcceptHeader(false)
                  .useRegisteredExtensionsOnly(false)
                  .defaultContentType(MediaType.APPLICATION_JSON)
                  .mediaType("json", MediaType.APPLICATION_JSON)
                  .mediaType("xml", MediaType.APPLICATION_JSON)
                  .mediaType("txt", MediaType.APPLICATION_JSON);
    }

    /**
     * Add static resource handlers for serving static content
     * Useful for serving a simple web UI or app
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // cache for 1 hour

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(7200); // cache for 2 hours

        registry.addResourceHandler("/docs/**")
                .addResourceLocations("classpath:/docs/")
                .setCachePeriod(86400);
    }

    /**
     * Configure view resolvers for potential web UI pages
     */
    @Override
    public void configureViewResolvers(@NonNull ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/views/", ".jsp");
    }

    /**
     * Configure default servlet handling for serving static content
     * that doesn't match any specific mapping
     */
    @Override
    public void configureDefaultServletHandling(@NonNull DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    /**
     * Add interceptors for loggin, authentication, or other cross-cutting concerns
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("api/health");
    }

    /**
     * Logging interceptor class for debugging
     */
    private static class RequestLoggingInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(@NonNull jakarta.servlet.http.HttpServletRequest request,
                                @NonNull jakarta.servlet.http.HttpServletResponse response,
                                @NonNull Object handler) {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String query = request.getQueryString();

            System.out.println("API Request: " + method + " " + uri + 
                                (query != null ? "?" + query : ""));

            return true; // Continue processing
        }

        @Override
        public void afterCompletion(@NonNull jakarta.servlet.http.HttpServletRequest request,
                                    @NonNull jakarta.servlet.http.HttpServletResponse response,
                                    @NonNull Object handler, @Nullable Exception ex) {
            int status = response.getStatus();
            String uri = request.getRequestURI();

            if (ex != null) {
                System.err.println("API Error for " + uri + ": " + ex.getMessage());
            }
            else {
                System.out.println("API Response" + status + " for " + uri);
            }
        }
    }

}
