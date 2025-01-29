package com.tss.wvms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
public class WvmsApiServerApplication implements WebMvcConfigurer {
	
	@Autowired
	private SecurityConfig interceptor;

	public static void main(String[] args) {
		SpringApplication.run(WvmsApiServerApplication.class, args);
	}

	 @Bean
	 WebMvcConfigurer corsConfigurer() {
	    return new WebMvcConfigurer() {
	       @Override
	       public void addCorsMappings(CorsRegistry registry) {
	          registry.addMapping("/**").allowedOrigins("*") // Allow requests from any origin
	                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // Allow specified methods
	                .allowedHeaders("*"); // Allow all headers
	       }
	    };
	 }
	 
	 @Override
	 public void addInterceptors(InterceptorRegistry registry) {
	    registry.addInterceptor(interceptor);
	 }

}
