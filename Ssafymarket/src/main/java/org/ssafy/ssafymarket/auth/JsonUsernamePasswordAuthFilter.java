package org.ssafy.ssafymarket.auth;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


public class JsonUsernamePasswordAuthFilter extends UsernamePasswordAuthenticationFilter {
	private final ObjectMapper om = new ObjectMapper();

	public JsonUsernamePasswordAuthFilter() {

		// 프론트가 호출할 로그인 URL
		setFilterProcessesUrl("/api/auth/login");
		// 기본 파라미터명(studentId, password)은 의미 없어짐(우리는 JSON에서 읽음)
	}

	@Override
	@SneakyThrows
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
		}

		String contentType = request.getContentType();
		String studentId;
		String password;

		if (contentType != null && contentType.contains("application/json")) {
			// JSON 바디 파싱
			Map<String, String> body = om.readValue(request.getInputStream(), Map.class);
			studentId = body.getOrDefault("studentId", "");
			password  = body.getOrDefault("password", "");
		} else {
			// (fallback) 폼이나 쿼리 파라미터에서 읽기
			studentId = request.getParameter("studentId");
			password  = request.getParameter("password");
			if (studentId == null) studentId = "";
			if (password == null)  password  = "";
		}

		studentId = studentId.trim();

		var authRequest = new UsernamePasswordAuthenticationToken(studentId, password);
		setDetails(request, authRequest);
		return this.getAuthenticationManager().authenticate(authRequest);
	}
}