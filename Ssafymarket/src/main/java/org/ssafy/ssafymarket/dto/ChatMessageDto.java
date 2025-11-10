package org.ssafy.ssafymarket.dto;

import lombok.*;
import org.ssafy.ssafymarket.entity.ChatMessage;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private Long messageId;
    private Long roomId;
    private String senderId;
    private String senderName;
    private String content;
    private ChatMessage.MessageType messageType;
    private String imageUrl;  // 이미지 URL (IMAGE 타입일 때 사용)
    private LocalDateTime sentAt;
    private Boolean isRead;
    private LocalDateTime readAt;

    // Entity to DTO
    public static ChatMessageDto fromEntity(ChatMessage message) {
        return ChatMessageDto.builder()
                .messageId(message.getMessageId())
                .roomId(message.getChatRoom().getRoomId())
                .senderId(message.getSender().getStudentId())
                .senderName(message.getSenderName())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .imageUrl(message.getImageUrl())
                .sentAt(message.getSentAt())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .build();
    }
}
