package com.xperience.hero.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Scopes CORS to the known frontend origin only (DESIGN.md Step 11: never wildcard). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.cors.allowed-origin}")
	private String allowedOrigin;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigin)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*");
	}
}
