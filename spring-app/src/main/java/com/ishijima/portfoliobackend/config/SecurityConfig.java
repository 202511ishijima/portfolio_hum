package com.ishijima.portfoliobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

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
				.requestMatchers("/api/cafe/menu", "/api/cafe/checkin", "/api/cafe/order-menu", "/api/cafe/orders", "/api/cafe/sessions/*").permitAll()
				.requestMatchers("/api/inquiries", "/api/inquiries/replies", "/api/inquiries/thread", "/api/members/register", "/api/members/points", "/api/members/status").permitAll()
				.requestMatchers("/admin/employees/new").hasRole("ADMIN")
				.requestMatchers("/admin/shifts/new").hasAnyRole("ADMIN", "STAFF_MANAGER")
				.requestMatchers("/admin/hamsters/new").hasAnyRole("ADMIN", "STAFF_MANAGER")
				.requestMatchers("/admin/products/orders/new", "/admin/products/inventory/**").hasAnyRole("ADMIN", "STAFF_MANAGER")
				.requestMatchers("/admin/cafe/orders/*/status", "/admin/cafe/menu/*/update", "/admin/cafe/reception", "/admin/cafe/sessions/*/checkout").hasAnyRole("ADMIN", "STAFF_MANAGER")
				.requestMatchers("/admin/**", "/api/admin/**").hasAnyRole("ADMIN", "STAFF_MANAGER", "STAFF", "VIEWER")
				.requestMatchers("/staff/**").hasAnyRole("ADMIN", "STAFF_MANAGER", "STAFF", "VIEWER")
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
				.loginPage("/admin/login")
				.loginProcessingUrl("/admin/login")
				.successHandler(this::loginSuccessHandler)
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

	private void loginSuccessHandler(
		jakarta.servlet.http.HttpServletRequest request,
		jakarta.servlet.http.HttpServletResponse response,
		Authentication authentication
	) throws java.io.IOException {
		boolean isStaffOnly = authentication.getAuthorities().stream()
			.map(authority -> authority.getAuthority())
			.anyMatch("ROLE_STAFF"::equals);

		if (isStaffOnly) {
			response.sendRedirect("/staff/shifts/mine");
			return;
		}
		response.sendRedirect("/admin/dashboard");
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
