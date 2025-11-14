package org.ssafy.ssafymarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message",
       indexes = {
           @Index(name = "idx_room_sent", columnList = "room_id, sent_at"),
           @Index(name = "idx_room_unread", columnList = "room_id, is_read")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", referencedColumnName = "student_id", nullable = false)
    private User sender;

    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    /**
     * 메시지 내용
     * - CHAT, PRICE_OFFER: 텍스트 메시지
     * - IMAGE: 이미지 설명 또는 빈 문자열
     * - ENTER, LEAVE, SYSTEM: 시스템 메시지
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    @Builder.Default
    private MessageType messageType = MessageType.CHAT;

    /**
     * 이미지 URL (messageType이 IMAGE일 때만 사용)
     * MinIO에 저장된 이미지 경로
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public enum MessageType {
        CHAT,        // 일반 채팅
        ENTER,       // 입장 알림
        LEAVE,       // 퇴장 알림
        PRICE_OFFER, // 가격 제안 (사용 안 함, DB 호환성 위해 유지)
        SYSTEM,      // 시스템 메시지
        IMAGE        // 이미지 메시지
    }
}
