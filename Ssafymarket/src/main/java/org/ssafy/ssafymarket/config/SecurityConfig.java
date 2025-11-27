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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.ssafy.ssafymarket.auth.JsonUsernamePasswordAuthFilter;

// CORS
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableJdbcHttpSession
@RequiredArgsConstructor
public class SecurityConfig {
	private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
	private final SpringSessionBackedSessionRegistry<? extends Session> springSessionBackedSessionRegistry;
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {

		JsonUsernamePasswordAuthFilter jsonLoginFilter = new JsonUsernamePasswordAuthFilter();
		jsonLoginFilter.setAuthenticationManager(authManager);
		// 로그인 성공 시: 세션 생성 보장 + JSON 응답
		jsonLoginFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
			var session =request.getSession(true);
			String currentSessionId=session.getId();
			String username = authentication.getName();

			var sessions = sessionRepository.findByPrincipalName(username);

			sessions.forEach((sessionId, s) -> {
				if (!sessionId.equals(currentSessionId)) {
					sessionRepository.deleteById(sessionId);

				}
			});


			request.getSession(true); // 세션 생성 (Set-Cookie 강제)
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
			// ★ CORS (프론트 사용 시)
			.cors(c -> {})
			.csrf(csrf -> csrf
				.ignoringRequestMatchers(
					"/ws/**", "/api/chat/**", "/api/test/**", "/api/auth/**",
					"/api/posts/**","/api/user/**","/api/admin/**"
				)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/**", "/ws/**", "/api/public/**", "/chat-test.html", "/post-test.html", "/api/test/**","/swagger-ui/**","/v3/api-docs/**").permitAll()
				// 채팅 API는 인증 필요
				.requestMatchers("/api/chat/**").authenticated()
				// 게시물 작성/수정/삭제는 인증 필요
				.requestMatchers(HttpMethod.POST,   "/api/posts/**").authenticated()
				.requestMatchers(HttpMethod.PUT,    "/api/posts/**").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/posts/**").authenticated()
				// 조회만 공개로 둘 거면 GET 허용
				.requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
				.requestMatchers("/api/user/**").authenticated()
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated()
			)
			// ★ (중요) 컨텍스트 저장을 명시적으로 요구하지 않게
			.securityContext(sc -> sc.requireExplicitSave(false))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)

			// ★ (중요) 필터 위치를 UsernamePasswordAuthenticationFilter 위치에 정확히 삽입
			.addFilterAt(jsonLoginFilter, UsernamePasswordAuthenticationFilter.class)

			.logout(logout -> logout
				.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler((request, response, authentication) -> {
					response.setStatus(200);
					response.setContentType("application/json;charset=UTF-8");
					response.getWriter().write("{\"success\":true,\"message\":\"로그아웃 성공\"}");
				})
				.invalidateHttpSession(true)
				.deleteCookies("SESSION") // Spring Session 기본 쿠키명
			)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				.maximumSessions(1)
				.maxSessionsPreventsLogin(false)
				.sessionRegistry(springSessionBackedSessionRegistry)
			);


		http.addFilterAt(jsonLoginFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// 프론트 오리진에 맞게 수정 가능
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();

		// ★ 모든 도메인 허용(자격증명 허용 시 반드시 patterns 사용)
		config.setAllowedOriginPatterns(List.of("*"));

		// 요청 메서드/헤더 전부 허용
		config.setAllowedMethods(List.of("*"));
		config.setAllowedHeaders(List.of("*"));

		// 쿠키/인증정보 허용 (세션 쿠키 필요하면 true)
		config.setAllowCredentials(true);

		// 노출할 헤더(필요시만)
		config.setExposedHeaders(List.of("Set-Cookie", "Authorization"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public org.springframework.session.web.http.CookieSerializer cookieSerializer() {
		var s = new org.springframework.session.web.http.DefaultCookieSerializer();
		s.setCookieName("SESSION");
		s.setUseHttpOnlyCookie(true);
		s.setUseSecureCookie(false); // HTTP 환경에서도 쿠키 전달
		return s;
	}
}
