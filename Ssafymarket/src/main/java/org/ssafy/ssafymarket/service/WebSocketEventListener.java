package org.ssafy.ssafymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    // 온라인 사용자 관리 (세션 ID → 사용자 ID)
    private final Map<String, String> onlineUsers = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결 이벤트
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = headerAccessor.getSessionId();
        String studentId = (String) headerAccessor.getSessionAttributes().get("studentId");

        if (studentId != null) {
            onlineUsers.put(sessionId, studentId);
            log.info("사용자 연결 - sessionId: {}, studentId: {}, 온라인 사용자 수: {}",
                    sessionId, studentId, onlineUsers.size());
        }
    }

    /**
     * WebSocket 연결 해제 이벤트
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = headerAccessor.getSessionId();
        String studentId = onlineUsers.remove(sessionId);

        if (studentId != null) {
            log.info("사용자 연결 해제 - sessionId: {}, studentId: {}, 온라인 사용자 수: {}",
                    sessionId, studentId, onlineUsers.size());
        }
    }

    /**
     * 특정 사용자가 온라인인지 확인
     */
    public boolean isUserOnline(String studentId) {
        return onlineUsers.containsValue(studentId);
    }

    /**
     * 온라인 사용자 수 조회
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }
}
