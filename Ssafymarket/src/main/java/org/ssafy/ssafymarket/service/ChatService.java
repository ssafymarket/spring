package org.ssafy.ssafymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssafy.ssafymarket.dto.ChatMessageDto;
import org.ssafy.ssafymarket.entity.ChatMessage;
import org.ssafy.ssafymarket.entity.ChatRoom;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.ChatMessageRepository;
import org.ssafy.ssafymarket.repository.ChatRoomRepository;
import org.ssafy.ssafymarket.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 메시지 전송 및 저장 (이미지 지원)
     */
    @Transactional
    public ChatMessageDto sendMessage(Long roomId, String senderId, String content,
                                       ChatMessage.MessageType messageType, String imageUrl) {
        // 채팅방 조회 (Fetch Join으로 성능 최적화)
        ChatRoom chatRoom = chatRoomRepository.findByIdWithFetch(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        // 발신자 조회
        User sender = userRepository.findByStudentId(senderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + senderId));

        // 권한 확인
        if (!chatRoom.getBuyer().getStudentId().equals(senderId) &&
            !chatRoom.getSeller().getStudentId().equals(senderId)) {
            throw new IllegalArgumentException("채팅방에 메시지를 보낼 권한이 없습니다");
        }

        // IMAGE 타입 검증
        if (messageType == ChatMessage.MessageType.IMAGE && (imageUrl == null || imageUrl.isBlank())) {
            throw new IllegalArgumentException("이미지 메시지는 imageUrl이 필수입니다");
        }

        // 메시지 저장
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .senderName(sender.getName())
                .content(content)
                .messageType(messageType)
                .imageUrl(imageUrl)
                .isRead(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 채팅방 최근 메시지 업데이트 (ENTER 타입은 제외, 이미지는 "사진"으로 표시)
        if (messageType != ChatMessage.MessageType.ENTER) {
            String lastMessagePreview = messageType == ChatMessage.MessageType.IMAGE ? "사진" : content;
            chatRoom.setLastMessage(lastMessagePreview);
            chatRoom.setLastMessageTime(savedMessage.getSentAt());
        }

        // 안읽은 메시지 카운트 증가
        boolean isBuyer = chatRoom.getBuyer().getStudentId().equals(senderId);
        if (isBuyer) {
            chatRoom.setUnreadSeller(chatRoom.getUnreadSeller() + 1);
        } else {
            chatRoom.setUnreadBuyer(chatRoom.getUnreadBuyer() + 1);
        }

        chatRoomRepository.save(chatRoom);

        log.info("메시지 저장 - roomId: {}, sender: {}, type: {}, hasImage: {}",
                roomId, senderId, messageType, imageUrl != null);

        // 상대방에게 실시간 알림 전송 (ENTER 메시지는 제외)
        if (messageType != ChatMessage.MessageType.ENTER) {
            String receiverId = isBuyer ? chatRoom.getSeller().getStudentId() : chatRoom.getBuyer().getStudentId();
            long receiverUnreadCount = chatMessageRepository.countTotalUnreadMessages(receiverId);

            messagingTemplate.convertAndSendToUser(
                    receiverId,
                    "/queue/notification",
                    java.util.Map.of(
                            "roomId", roomId,
                            "postId", chatRoom.getPost().getPostId(),
                            "postTitle", chatRoom.getPost().getTitle(),
                            "senderName", sender.getName(),
                            "content", messageType == ChatMessage.MessageType.IMAGE ? "사진" : content,
                            "totalUnreadCount", receiverUnreadCount,
                            "timestamp", savedMessage.getSentAt()
                    )
            );

            log.info("실시간 알림 전송 - receiverId: {}, totalUnreadCount: {}", receiverId, receiverUnreadCount);
        }

        return ChatMessageDto.fromEntity(savedMessage);
    }

    /**
     * 메시지 전송 및 저장 (하위 호환성)
     */
    @Transactional
    public ChatMessageDto sendMessage(Long roomId, String senderId, String content,
                                       ChatMessage.MessageType messageType) {
        return sendMessage(roomId, senderId, content, messageType, null);
    }

    /**
     * 채팅방 메시지 히스토리 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(Long roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ChatMessage> messages = chatMessageRepository
                .findByChatRoom_RoomIdOrderBySentAtDesc(roomId, pageable);

        return messages.stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 읽음 처리
     */
    @Transactional
    public void markMessagesAsRead(Long roomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithFetch(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        // 안읽은 메시지 읽음 처리
        int updatedCount = chatMessageRepository.markMessagesAsRead(roomId, userId);

        // 안읽은 메시지 카운트 초기화
        boolean isBuyer = chatRoom.getBuyer().getStudentId().equals(userId);
        if (isBuyer) {
            chatRoom.setUnreadBuyer(0);
        } else {
            chatRoom.setUnreadSeller(0);
        }

        chatRoomRepository.save(chatRoom);

        log.info("메시지 읽음 처리 - roomId: {}, userId: {}, count: {}", roomId, userId, updatedCount);

        // 상대방에게 읽음 알림 전송
        String otherUserId = isBuyer ? chatRoom.getSeller().getStudentId() : chatRoom.getBuyer().getStudentId();
        messagingTemplate.convertAndSendToUser(
                otherUserId,
                "/queue/read",
                ChatMessageDto.builder()
                        .roomId(roomId)
                        .messageType(ChatMessage.MessageType.SYSTEM)
                        .content("상대방이 메시지를 읽었습니다")
                        .build()
        );
    }

    /**
     * 안읽은 메시지 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long roomId, String userId) {
        return chatMessageRepository.countUnreadMessages(roomId, userId);
    }

    /**
     * 전체 안읽은 메시지 개수 조회
     */
    @Transactional(readOnly = true)
    public long getTotalUnreadCount(String userId) {
        return chatMessageRepository.countTotalUnreadMessages(userId);
    }
}
