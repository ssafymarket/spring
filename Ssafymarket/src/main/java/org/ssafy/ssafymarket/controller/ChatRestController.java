package org.ssafy.ssafymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.ssafy.ssafymarket.dto.ChatMessageDto;
import org.ssafy.ssafymarket.dto.ChatRoomCreateRequest;
import org.ssafy.ssafymarket.dto.ChatRoomDto;
import org.ssafy.ssafymarket.service.ChatRoomService;
import org.ssafy.ssafymarket.service.ChatService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;

    /**
     * 채팅방 생성 또는 조회
     * POST /api/chat/room/create
     */
    @PostMapping("/room/create")
    public ResponseEntity<ChatRoomDto> createChatRoom(
            @RequestBody ChatRoomCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String buyerId = userDetails.getUsername();
        ChatRoomDto chatRoom = chatRoomService.createOrGetChatRoom(buyerId, request.getPostId());

        log.info("채팅방 생성/조회 - postId: {}, buyerId: {}, roomId: {}",
                request.getPostId(), buyerId, chatRoom.getRoomId());

        return ResponseEntity.ok(chatRoom);
    }

    /**
     * 내 채팅방 목록 조회
     * GET /api/chat/rooms
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getMyChatRooms(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        List<ChatRoomDto> chatRooms = chatRoomService.getUserChatRooms(userId);

        log.info("채팅방 목록 조회 - userId: {}, count: {}", userId, chatRooms.size());

        return ResponseEntity.ok(chatRooms);
    }

    /**
     * 특정 채팅방 상세 조회
     * GET /api/chat/room/{roomId}
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ChatRoomDto> getChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        ChatRoomDto chatRoom = chatRoomService.getChatRoom(roomId, userId);

        return ResponseEntity.ok(chatRoom);
    }

    /**
     * 채팅방 메시지 히스토리 조회
     * GET /api/chat/room/{roomId}/messages?page=0&size=50
     */
    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();

        // 권한 확인
        chatRoomService.getChatRoom(roomId, userId);

        List<ChatMessageDto> messages = chatService.getMessages(roomId, page, size);

        log.info("메시지 히스토리 조회 - roomId: {}, page: {}, size: {}, count: {}",
                roomId, page, size, messages.size());

        return ResponseEntity.ok(messages);
    }

    /**
     * 메시지 읽음 처리
     * PUT /api/chat/room/{roomId}/read
     */
    @PutMapping("/room/{roomId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        chatService.markMessagesAsRead(roomId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "메시지 읽음 처리 완료");

        return ResponseEntity.ok(response);
    }

    /**
     * 안읽은 메시지 개수 조회
     * GET /api/chat/room/{roomId}/unread-count
     */
    @GetMapping("/room/{roomId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        long unreadCount = chatService.getUnreadCount(roomId, userId);

        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    /**
     * 전체 안읽은 메시지 개수 조회
     * GET /api/chat/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getTotalUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        long totalUnreadCount = chatService.getTotalUnreadCount(userId);

        Map<String, Long> response = new HashMap<>();
        response.put("totalUnreadCount", totalUnreadCount);

        return ResponseEntity.ok(response);
    }
}
