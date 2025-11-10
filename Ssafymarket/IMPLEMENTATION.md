# WebSocket 채팅 기능 구현 명세서

## 프로젝트 개요

- 프로젝트명: Ssafymarket
- 기능: 1:1 실시간 채팅 시스템
- 인증 방식: Spring Security 세션 기반
- 데이터베이스: MySQL + JPA
- 통신 프로토콜: WebSocket (STOMP)

---

## 구현 범위

### 1. Dependencies (build.gradle)

추가된 의존성:
- spring-boot-starter-websocket
- spring-session-jdbc
- lombok
- 기존 의존성: spring-boot-starter-security, spring-boot-starter-web, spring-boot-starter-data-jpa, mysql-connector-j

파일 위치: `C:\ssafymarket\Ssafymarket\build.gradle`

---

### 2. Entity 클래스 (총 6개)

#### 2.1 User Entity
- 파일: `src/main/java/org/ssafy/ssafymarket/entity/User.java`
- 테이블명: user
- 주요 필드:
  - studentId (PK, String)
  - name (String, nullable=false)
  - className (String)
  - password (String, nullable=false)
  - role (Enum: ROLE_USER, ROLE_ADMIN)

#### 2.2 Post Entity
- 파일: `src/main/java/org/ssafy/ssafymarket/entity/Post.java`
- 테이블명: post
- 주요 필드:
  - postId (PK, Long, Auto Increment)
  - title (String, nullable=false)
  - price (Integer, nullable=false)
  - category (String)
  - createdAt (LocalDateTime, auto)
  - chatRoomCount (Integer, default=0)
  - likeCount (Integer, default=0)
  - status (Enum: 판매중, 판매완료)
  - writer (FK → User.studentId)
  - imageUrl (String)

#### 2.3 PostLike Entity
- 파일: `src/main/java/org/ssafy/ssafymarket/entity/PostLike.java`
- 테이블명: post_like
- 복합키: (userId, postId)
- 주요 필드:
  - userId (FK → User.studentId)
  - postId (FK → Post.postId)
  - likedAt (LocalDateTime, auto)

#### 2.4 ChatRoom Entity
- 파일: `src/main/java/org/ssafy/ssafymarket/entity/ChatRoom.java`
- 테이블명: chat_room
- UNIQUE 제약: (post_id, buyer_id)
- 주요 필드:
  - roomId (PK, Long, Auto Increment)
  - post (FK → Post)
  - buyer (FK → User.studentId)
  - seller (FK → User.studentId)
  - createdAt (LocalDateTime, auto)
  - lastMessage (Text)
  - lastMessageTime (LocalDateTime)
  - unreadBuyer (Integer, default=0)
  - unreadSeller (Integer, default=0)

#### 2.5 ChatMessage Entity
- 파일: `src/main/java/org/ssafy/ssafymarket/entity/ChatMessage.java`
- 테이블명: chat_message
- 인덱스: (room_id, sent_at), (room_id, is_read)
- 주요 필드:
  - messageId (PK, Long, Auto Increment)
  - chatRoom (FK → ChatRoom)
  - sender (FK → User.studentId)
  - senderName (String, nullable=false)
  - content (Text, nullable=false)
  - messageType (Enum: CHAT, ENTER, LEAVE, PRICE_OFFER, SYSTEM)
  - sentAt (LocalDateTime, auto)
  - isRead (Boolean, default=false)
  - readAt (LocalDateTime)

#### 2.6 Trade Entity
- 파일: `src/main/java/org/ssafy/ssafymarket/entity/Trade.java`
- 테이블명: trade
- 주요 필드:
  - tradeId (PK, Long, Auto Increment)
  - post (FK → Post)
  - buyer (FK → User.studentId)
  - seller (FK → User.studentId)
  - agreedPrice (Integer, nullable=false)
  - agreedAt (LocalDateTime, auto)

---

### 3. Repository 인터페이스 (총 6개)

#### 3.1 UserRepository
- 파일: `src/main/java/org/ssafy/ssafymarket/repository/UserRepository.java`
- 메서드:
  - findByStudentId(String studentId): Optional<User>
  - existsByStudentId(String studentId): boolean

#### 3.2 PostRepository
- 파일: `src/main/java/org/ssafy/ssafymarket/repository/PostRepository.java`
- 메서드:
  - findByWriter(User writer): List<Post>
  - findByStatus(PostStatus status): List<Post>
  - findByWriterStudentIdOrderByCreatedAtDesc(String studentId): List<Post>

#### 3.3 PostLikeRepository
- 파일: `src/main/java/org/ssafy/ssafymarket/repository/PostLikeRepository.java`
- 메서드:
  - findByUserIdAndPostId(String userId, Long postId): Optional<PostLike>
  - countByPostId(Long postId): long
  - existsByUserIdAndPostId(String userId, Long postId): boolean

#### 3.4 ChatRoomRepository
- 파일: `src/main/java/org/ssafy/ssafymarket/repository/ChatRoomRepository.java`
- 메서드:
  - findByPost_PostIdAndBuyer_StudentId(Long postId, String buyerId): Optional<ChatRoom>
  - findAllByUserId(String userId): List<ChatRoom> (JPQL 쿼리)
  - findByBuyer_StudentIdOrderByLastMessageTimeDesc(String buyerId): List<ChatRoom>
  - findBySeller_StudentIdOrderByLastMessageTimeDesc(String sellerId): List<ChatRoom>
  - countByPost_PostId(Long postId): long

#### 3.5 ChatMessageRepository
- 파일: `src/main/java/org/ssafy/ssafymarket/repository/ChatMessageRepository.java`
- 메서드:
  - findByChatRoom_RoomIdOrderBySentAtDesc(Long roomId): List<ChatMessage>
  - findByChatRoom_RoomIdOrderBySentAtDesc(Long roomId, Pageable): List<ChatMessage>
  - countUnreadMessages(Long roomId, String userId): long (JPQL 쿼리)
  - markMessagesAsRead(Long roomId, String userId): int (@Modifying JPQL)
  - findTopByChatRoom_RoomIdOrderBySentAtDesc(Long roomId): ChatMessage
  - countTotalUnreadMessages(String userId): long (JPQL 쿼리)

#### 3.6 TradeRepository
- 파일: `src/main/java/org/ssafy/ssafymarket/repository/TradeRepository.java`
- 메서드:
  - findByPost_PostId(Long postId): Optional<Trade>
  - existsByPost_PostId(Long postId): boolean

---

### 4. DTO 클래스 (총 4개)

#### 4.1 ChatMessageDto
- 파일: `src/main/java/org/ssafy/ssafymarket/dto/ChatMessageDto.java`
- 필드: messageId, roomId, senderId, senderName, content, messageType, sentAt, isRead, readAt
- 메서드: fromEntity(ChatMessage) - Entity to DTO 변환

#### 4.2 ChatRoomDto
- 파일: `src/main/java/org/ssafy/ssafymarket/dto/ChatRoomDto.java`
- 필드: roomId, postId, postTitle, postImage, postPrice, buyerId, buyerName, sellerId, sellerName, lastMessage, lastMessageTime, unreadCount, iAmBuyer, createdAt
- 메서드: fromEntity(ChatRoom, String currentUserId) - Entity to DTO 변환

#### 4.3 ChatRoomCreateRequest
- 파일: `src/main/java/org/ssafy/ssafymarket/dto/ChatRoomCreateRequest.java`
- 필드: postId (Long)

#### 4.4 ChatMessageRequest
- 파일: `src/main/java/org/ssafy/ssafymarket/dto/ChatMessageRequest.java`
- 필드: content (String), messageType (ChatMessage.MessageType)

---

### 5. Configuration 클래스 (총 3개)

#### 5.1 SecurityConfig
- 파일: `src/main/java/org/ssafy/ssafymarket/config/SecurityConfig.java`
- 어노테이션: @Configuration, @EnableWebSecurity, @EnableJdbcHttpSession
- 설정 내용:
  - CSRF: WebSocket 엔드포인트(/ws/**)는 제외
  - 인증 불필요 경로: /api/auth/**, /ws/**, /api/public/**
  - 관리자 경로: /api/admin/** (ROLE_ADMIN 필요)
  - 로그인 엔드포인트: POST /api/auth/login
  - 로그아웃 엔드포인트: POST /api/auth/logout
  - 세션 관리: 최대 1개 세션, 새 로그인 시 기존 세션 무효화
- Bean:
  - PasswordEncoder: BCryptPasswordEncoder
  - AuthenticationManager

#### 5.2 WebSocketConfig
- 파일: `src/main/java/org/ssafy/ssafymarket/config/WebSocketConfig.java`
- 어노테이션: @Configuration, @EnableWebSocketMessageBroker
- 설정 내용:
  - STOMP 엔드포인트: /ws (SockJS fallback 지원)
  - Message Broker: /topic, /queue
  - Application Destination Prefix: /app
  - User Destination Prefix: /user
  - Interceptor: HttpHandshakeInterceptor 등록

#### 5.3 HttpHandshakeInterceptor
- 파일: `src/main/java/org/ssafy/ssafymarket/config/HttpHandshakeInterceptor.java`
- 어노테이션: @Component
- 기능:
  - WebSocket 연결 전 HTTP 세션 확인
  - SecurityContext에서 인증 정보 추출
  - WebSocket 세션에 studentId, sessionId 저장
  - 인증되지 않은 사용자는 연결 거부

---

### 6. Service 클래스 (총 3개)

#### 6.1 CustomUserDetailsService
- 파일: `src/main/java/org/ssafy/ssafymarket/service/CustomUserDetailsService.java`
- 어노테이션: @Service
- 구현 인터페이스: UserDetailsService
- 메서드:
  - loadUserByUsername(String studentId): UserDetails
    - 학번으로 사용자 조회
    - 권한 정보 포함하여 UserDetails 반환

#### 6.2 ChatRoomService
- 파일: `src/main/java/org/ssafy/ssafymarket/service/ChatRoomService.java`
- 어노테이션: @Service
- 메서드:
  - createOrGetChatRoom(String buyerId, Long postId): ChatRoomDto
    - 채팅방 생성 또는 기존 채팅방 반환
    - 본인 게시글 검증
    - 게시글의 chatRoomCount 자동 증가
  - getUserChatRooms(String userId): List<ChatRoomDto>
    - 사용자 참여 채팅방 목록 조회
  - getChatRoom(Long roomId, String userId): ChatRoomDto
    - 특정 채팅방 조회 및 권한 검증
  - existsChatRoom(Long roomId): boolean

#### 6.3 ChatService
- 파일: `src/main/java/org/ssafy/ssafymarket/service/ChatService.java`
- 어노테이션: @Service
- 메서드:
  - sendMessage(Long roomId, String senderId, String content, MessageType): ChatMessageDto
    - 메시지 DB 저장
    - 채팅방의 lastMessage, lastMessageTime 업데이트
    - 상대방의 안읽은 메시지 카운트 증가
  - getMessages(Long roomId, int page, int size): List<ChatMessageDto>
    - 페이징된 메시지 히스토리 조회
  - markMessagesAsRead(Long roomId, String userId): void
    - 안읽은 메시지를 읽음 처리
    - 채팅방의 안읽은 카운트 초기화
    - 상대방에게 읽음 알림 전송 (/queue/read)
  - getUnreadCount(Long roomId, String userId): long
  - getTotalUnreadCount(String userId): long

---

### 7. Controller 클래스 (총 2개)

#### 7.1 ChatWebSocketController
- 파일: `src/main/java/org/ssafy/ssafymarket/controller/ChatWebSocketController.java`
- 어노테이션: @Controller
- WebSocket 메시지 매핑:
  - @MessageMapping("/chat/send/{roomId}") → @SendTo("/topic/room/{roomId}")
    - 메시지 전송 및 브로드캐스트
    - 세션에서 senderId 추출
  - @MessageMapping("/chat/enter/{roomId}") → @SendTo("/topic/room/{roomId}")
    - 채팅방 입장 알림
  - @MessageMapping("/chat/read/{roomId}")
    - 메시지 읽음 처리

#### 7.2 ChatRestController
- 파일: `src/main/java/org/ssafy/ssafymarket/controller/ChatRestController.java`
- 어노테이션: @RestController, @RequestMapping("/api/chat")
- REST API 엔드포인트:
  - POST /api/chat/room/create
    - 요청: ChatRoomCreateRequest (postId)
    - 응답: ChatRoomDto
  - GET /api/chat/rooms
    - 응답: List<ChatRoomDto>
  - GET /api/chat/room/{roomId}
    - 응답: ChatRoomDto
  - GET /api/chat/room/{roomId}/messages?page=0&size=50
    - 응답: List<ChatMessageDto>
  - PUT /api/chat/room/{roomId}/read
    - 응답: {success: boolean, message: String}
  - GET /api/chat/room/{roomId}/unread-count
    - 응답: {unreadCount: long}
  - GET /api/chat/unread-count
    - 응답: {totalUnreadCount: long}

---

### 8. Event Listener

#### WebSocketEventListener
- 파일: `src/main/java/org/ssafy/ssafymarket/service/WebSocketEventListener.java`
- 어노테이션: @Component
- 기능:
  - SessionConnectedEvent 처리: 사용자 온라인 상태 관리
  - SessionDisconnectEvent 처리: 사용자 연결 해제 처리
  - onlineUsers: ConcurrentHashMap<String sessionId, String studentId>
- 메서드:
  - isUserOnline(String studentId): boolean
  - getOnlineUserCount(): int

---

### 9. Application 설정

#### application.yml
- 파일: `src/main/resources/application.yml`
- 추가된 설정:
  - spring.session.store-type: jdbc
  - spring.session.jdbc.initialize-schema: always
  - spring.session.timeout: 1800 (30분)
  - logging.level.org.ssafy.ssafymarket: DEBUG
  - logging.level.org.springframework.web.socket: DEBUG
  - logging.level.org.springframework.messaging: DEBUG

---

## 데이터베이스 스키마

### 자동 생성 테이블 (JPA)
1. user
2. post
3. post_like
4. chat_room (UNIQUE: post_id + buyer_id)
5. chat_message (INDEX: room_id + sent_at, room_id + is_read)
6. trade

### Spring Session 테이블 (자동 생성)
1. SPRING_SESSION
2. SPRING_SESSION_ATTRIBUTES

---

## 기술 스택

- Spring Boot: 3.5.7
- Java: 17
- Database: MySQL
- ORM: Spring Data JPA (Hibernate)
- Security: Spring Security (Session-based)
- WebSocket: Spring WebSocket + STOMP
- Session Store: Spring Session JDBC
- Build Tool: Gradle

---

## 구현되지 않은 부분

1. 회원가입 API
2. 게시글 CRUD API
3. 좋아요 기능 API
4. 거래 확정 API
5. 프론트엔드 UI
6. 이미지/파일 전송 기능
7. 푸시 알림
8. 메시지 검색 기능
9. 사용자 차단 기능

---

## 파일 구조

```
src/main/java/org/ssafy/ssafymarket/
├── config/
│   ├── HttpHandshakeInterceptor.java
│   ├── SecurityConfig.java
│   └── WebSocketConfig.java
├── controller/
│   ├── ChatRestController.java
│   └── ChatWebSocketController.java
├── dto/
│   ├── ChatMessageDto.java
│   ├── ChatMessageRequest.java
│   ├── ChatRoomCreateRequest.java
│   └── ChatRoomDto.java
├── entity/
│   ├── ChatMessage.java
│   ├── ChatRoom.java
│   ├── Post.java
│   ├── PostLike.java
│   ├── Trade.java
│   └── User.java
├── repository/
│   ├── ChatMessageRepository.java
│   ├── ChatRoomRepository.java
│   ├── PostLikeRepository.java
│   ├── PostRepository.java
│   ├── TradeRepository.java
│   └── UserRepository.java
├── service/
│   ├── ChatRoomService.java
│   ├── ChatService.java
│   ├── CustomUserDetailsService.java
│   └── WebSocketEventListener.java
└── SsafymarketApplication.java
```

---

## 환경 변수

필수 환경 변수:
- DB_URL: JDBC URL
- DB_USERNAME: 데이터베이스 사용자명
- DB_PASSWORD: 데이터베이스 비밀번호

---

## 작성일

2025-11-10
