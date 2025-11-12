package org.ssafy.ssafymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.ssafy.ssafymarket.dto.ChatMessageDto;
import org.ssafy.ssafymarket.dto.ChatMessageRequest;
import org.ssafy.ssafymarket.entity.ChatMessage;
import org.ssafy.ssafymarket.service.ChatService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지 전송
     * 클라이언트 → /app/chat/send/{roomId}
     * 브로드캐스트 → /topic/room/{roomId}
     */

    @MessageMapping("/chat/send/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessageDto sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        // WebSocket 세션에서 사용자 ID 추출
        String senderId = (String) headerAccessor.getSessionAttributes().get("studentId");

        if (senderId == null) {
            log.error("인증되지 않은 사용자의 메시지 전송 시도");
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }

        // 메시지 타입이 null이면 기본값 설정
        ChatMessage.MessageType messageType = request.getMessageType() != null ?
                request.getMessageType() : ChatMessage.MessageType.CHAT;

        // 메시지 저장 및 전송 (imageUrl 포함)
        ChatMessageDto message = chatService.sendMessage(
                roomId,
                senderId,
                request.getContent(),
                messageType,
                request.getImageUrl()
        );

        log.info("WebSocket 메시지 전송 - roomId: {}, sender: {}, type: {}, hasImage: {}",
                roomId, senderId, messageType, request.getImageUrl() != null);

        return message;
    }

    /**
     * 채팅방 입장
     * 클라이언트 → /app/chat/enter/{roomId}
     *
     * 입장 메시지는 DB에 저장하지 않고 WebSocket으로만 브로드캐스트
     */
    @MessageMapping("/chat/enter/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessageDto enterChatRoom(
            @DestinationVariable Long roomId,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = (String) headerAccessor.getSessionAttributes().get("studentId");

        if (userId == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }

        // 입장 메시지는 DB에 저장하지 않고 WebSocket으로만 전송
        ChatMessageDto enterMessage = ChatMessageDto.builder()
                .roomId(roomId)
                .senderId(userId)
                .senderName(userId)
                .content("님이 입장하셨습니다.")
                .messageType(ChatMessage.MessageType.ENTER)
                .isRead(true)
                .sentAt(java.time.LocalDateTime.now())
                .build();

        log.info("사용자 채팅방 입장 (DB 저장 안 함) - roomId: {}, userId: {}", roomId, userId);

        return enterMessage;
    }

    /**
     * 메시지 읽음 처리
     * 클라이언트 → /app/chat/read/{roomId}
     */
    @MessageMapping("/chat/read/{roomId}")
    public void markAsRead(
            @DestinationVariable Long roomId,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = (String) headerAccessor.getSessionAttributes().get("studentId");

        if (userId == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }

        // 메시지 읽음 처리 (서비스에서 읽음 알림도 전송)
        chatService.markMessagesAsRead(roomId, userId);

        log.info("메시지 읽음 처리 - roomId: {}, userId: {}", roomId, userId);
    }
}
