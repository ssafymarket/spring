package org.ssafy.ssafymarket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.ssafy.ssafymarket.dto.ChatMessageDto;
import org.ssafy.ssafymarket.dto.ChatRoomCreateRequest;
import org.ssafy.ssafymarket.dto.ChatRoomDto;
import org.ssafy.ssafymarket.minio.MinioService;
import org.ssafy.ssafymarket.service.ChatRoomService;
import org.ssafy.ssafymarket.service.ChatService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Tag(name = "채팅방", description = "채팅방 API")
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final MinioService minioService;

    /**
     * 채팅방 생성 또는 조회
     * POST /api/chat/room/create
     */
	@Operation(
		summary = "채팅방 생성/조회",
		description = "postId 기반으로 채팅방을 생성한다."
	)
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
	@Operation(
		summary = "내 채팅방 목록 조회"
	)
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


	@Operation(
		summary = "특정 채팅방 상세 조회",
		description = "roomId 기반으로 채팅방 상세 조회"
	)
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
	@Operation(
		summary = "채팅방 메시지 히스토리 조회",
		description = "roomId 기반으로 채팅방 메시지 조회."
	)
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
	@Operation(
		summary = "메시지 읽음 처리",
		description = "roomid로 읽음 처리"
	)
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
	@Operation(
		summary = "안읽은 메시지 개수 조회",
		description = "roomid 기반으로 조히"
	)
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
	@Operation(
		summary = "전체 안읽은 메시지 개수 조회",
		description = ""
	)
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getTotalUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        long totalUnreadCount = chatService.getTotalUnreadCount(userId);

        Map<String, Long> response = new HashMap<>();
        response.put("totalUnreadCount", totalUnreadCount);

        return ResponseEntity.ok(response);
    }

    /**
     * 채팅방 나가기
     * DELETE /api/chat/room/{roomId}
     */
	@Operation(
		summary = "채팅방 나가기",
		description = "학번 ,이름,반,비밀 번호를 입력하여 회원가입을 진행한다."
	)
    @DeleteMapping("/room/{roomId}")
    public ResponseEntity<Map<String, Object>> leaveChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();

        try {
            chatRoomService.leaveChatRoom(roomId, userId);

            log.info("채팅방 나가기 성공 - roomId: {}, userId: {}", roomId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "채팅방을 나갔습니다."
            ));
        } catch (IllegalArgumentException e) {
            log.warn("채팅방 나가기 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("채팅방 나가기 실패 - 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "채팅방 나가기 실패: " + e.getMessage()
                    ));
        }
    }

    /**
     * 채팅 이미지 업로드
     * POST /api/chat/upload-image
     *
     * 사용 플로우:
     * 1. 클라이언트가 이 API로 이미지를 먼저 업로드
     * 2. 서버가 MinIO에 저장 후 imageUrl 반환
     * 3. 클라이언트가 WebSocket으로 메시지 전송:
     *    { messageType: "IMAGE", imageUrl: "반환받은URL", content: "" }
     *
     * @param file 업로드할 이미지 파일 (최대 10MB)
     * @return 업로드된 이미지 URL
     */
	@Operation(
		summary = "채팅 이미지 업로드",
		description = "file 업로드"
			+ "\n"
			+ " 1. 클라이언트가 이 API로 이미지를 먼저 업로드\n"
			+ "2. 서버가 MinIO에 저장 후 imageUrl 반환\n"
			+ "     3. 클라이언트가 WebSocket으로 메시지 전송:\n"
			+ "        { messageType: \"IMAGE\", imageUrl: \"반환받은URL\", content: \"\" }"
	)
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadChatImage(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        log.info("채팅 이미지 업로드 요청 - userId: {}, fileName: {}, size: {} bytes",
                userId, file.getOriginalFilename(), file.getSize());

        try {
            // MinIO에 이미지 업로드
            String imageUrl = minioService.uploadChatImage(file);

            log.info("채팅 이미지 업로드 성공 - userId: {}, imageUrl: {}", userId, imageUrl);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "imageUrl", imageUrl,
                    "message", "이미지 업로드 성공"
            ));

        } catch (IllegalArgumentException e) {
            // 파일 검증 실패 (빈 파일, 크기 초과 등)
            log.warn("채팅 이미지 업로드 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {
            // MinIO 업로드 실패 등
            log.error("채팅 이미지 업로드 실패 - 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "이미지 업로드 실패: " + e.getMessage()
                    ));
        }
    }
}
