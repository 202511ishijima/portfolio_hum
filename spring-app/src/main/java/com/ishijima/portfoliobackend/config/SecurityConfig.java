package com.ishijima.portfoliobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	private final AdminLoginSuccessHandler adminLoginSuccessHandler;

	public SecurityConfig(AdminLoginSuccessHandler adminLoginSuccessHandler) {
		this.adminLoginSuccessHandler = adminLoginSuccessHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**", "/api/**")
			)
			.headers(headers -> headers
				.frameOptions(frame -> frame.sameOrigin())
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/error", "/admin.css", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
				.requestMatchers("/admin/login").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.requestMatchers("/api/hamsters/summary").permitAll()
				.requestMatchers("/api/products/stocks", "/api/products/purchase").permitAll()
				.requestMatchers(
					"/api/cafe/menu",
					"/api/cafe/checkin",
					"/api/cafe/order-menu",
					"/api/cafe/orders",
					"/api/cafe/orders/history",
					"/api/cafe/sessions/*",
					"/api/cafe/sessions/*/checkout"
				).permitAll()
				.requestMatchers("/api/inquiries", "/api/inquiries/replies", "/api/inquiries/thread", "/api/members/register", "/api/members/points", "/api/members/status").permitAll()
				.requestMatchers("/admin/**", "/api/admin/**").hasAnyRole("ADMIN", "STAFF_MANAGER", "STAFF", "VIEWER")
				.requestMatchers("/staff/**").hasAnyRole("ADMIN", "STAFF_MANAGER", "STAFF", "VIEWER")
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
				.loginPage("/admin/login")
				.loginProcessingUrl("/admin/login")
				.successHandler(adminLoginSuccessHandler)
				.failureUrl("/admin/login?error")
				.permitAll()
			)
			.logout(logout -> logout
				.logoutUrl("/admin/logout")
				.logoutSuccessUrl("/admin/login?logout")
			)
			.httpBasic(Customizer.withDefaults());

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
