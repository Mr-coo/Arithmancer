package com.arithmancer.web;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// The built frontend is served from static/. Vite puts a hash in the names of its files under /assets, so they never
// change and browsers can keep them; everything else is checked on every load (see application.properties).
@Configuration
public class StaticFilesConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/assets/**")
				.addResourceLocations("classpath:/static/assets/")
				.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
	}

}
