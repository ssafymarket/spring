package org.ssafy.ssafymarket.config;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpSession session = servletRequest.getServletRequest().getSession(false);

            if (session != null) {
                // 세션에서 SecurityContext 추출
                SecurityContext securityContext = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");

                if (securityContext != null) {
                    Authentication authentication = securityContext.getAuthentication();

                    if (authentication != null && authentication.isAuthenticated()) {
                        String studentId = authentication.getName();
                        attributes.put("studentId", studentId);
                        attributes.put("sessionId", session.getId());
                        log.info("WebSocket 연결 - 사용자: {}, 세션: {}", studentId, session.getId());
                        return true;
                    }
                }
            }

            log.warn("WebSocket 연결 실패 - 인증되지 않은 사용자");
            return false; // 인증되지 않은 경우 연결 거부
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 이후 처리 (필요시 구현)
    }
}
