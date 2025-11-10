package org.ssafy.ssafymarket.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.ssafy.ssafymarket.auth.JsonUsernamePasswordAuthFilter;

@Configuration
@EnableWebSecurity
@EnableJdbcHttpSession
@RequiredArgsConstructor
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {

		JsonUsernamePasswordAuthFilter jsonLoginFilter = new JsonUsernamePasswordAuthFilter();
		jsonLoginFilter.setAuthenticationManager(authManager);
		// 로그인 성공 시
		jsonLoginFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json;charset=UTF-8");
			String userId = authentication.getName();
			var roles = authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList();
			response.getWriter().write("""
                {"success": true, "message": "로그인 성공", "userId": "%s", "roles": %s}
            """.formatted(userId, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(roles)));
		});

		// 로그인 실패 시
		jsonLoginFilter.setAuthenticationFailureHandler((request, response, ex) -> {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\": false, \"message\": \"로그인 실패\"}");
		});

		http
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/ws/**", "/api/chat/**", "/api/test/**", "/api/auth/**")
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/**", "/ws/**", "/api/public/**", "/chat-test.html", "/api/chat/**", "/api/test/**").permitAll()
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated()
			)

			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)

			.addFilterAt(jsonLoginFilter, JsonUsernamePasswordAuthFilter.class)
			.logout(logout -> logout
				.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler((request, response, authentication) -> {
					response.setStatus(200);
					response.setContentType("application/json;charset=UTF-8");
					response.getWriter().write("{\"success\":true,\"message\":\"로그아웃 성공\"}");
				})
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
			)
			.sessionManagement(session -> session
				.maximumSessions(1)
				.maxSessionsPreventsLogin(false)
			);

		return http.build();
	}


	@Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
