# 구현 내용 정리

작성일: 2025-11-11

## 금일 구현 내역

### 1. 채팅 관련 API 구현

#### 1.1 채팅방 목록 조회
- 엔드포인트: GET /api/chat/rooms
- 기능: 사용자가 참여 중인 모든 채팅방 목록 조회
- 구현 위치: ChatRestController.java:54, ChatRoomService.java:74

#### 1.2 채팅 메시지 읽음 처리
- 엔드포인트: PUT /api/chat/room/{roomId}/read
- 기능: 특정 채팅방의 안읽은 메시지를 읽음으로 처리
- 구현 위치: ChatRestController.java:109, ChatService.java

#### 1.3 채팅방 나가기
- 엔드포인트: DELETE /api/chat/room/{roomId}
- 기능: 채팅방 삭제 및 게시글의 채팅방 카운트 감소
- 구현 위치: ChatRestController.java:163, ChatRoomService.java:108

### 2. 게시글 검색 API 구현

#### 2.1 검색 기능
- 엔드포인트: GET /api/posts/search
- 기능: 제목 및 설명 필드에서 키워드 검색
- 검색 방식: 대소문자 무시, 부분 일치
- 필터링: 선택적 판매상태 필터 (판매중/판매완료/전체)
- 정렬: latest, popular, lowPrice, highPrice 지원
- 페이징: page, size 파라미터 지원
- 구현 위치: PostController.java:976, PostRepository.java:28-41

### 3. 컨트롤러 리팩토링

기존 PostController (965줄)를 기능별로 3개 파일로 분리

#### 3.1 PostController.java
- 책임: 게시글 기본 CRUD 및 검색
- 유지된 엔드포인트:
  - POST /api/posts
  - GET /api/posts
  - GET /api/posts/{postId}
  - PUT /api/posts/{postId}
  - DELETE /api/posts/{postId}
  - GET /api/posts/category/{category}
  - GET /api/posts/status/{status}
  - GET /api/posts/user/{studentId}
  - GET /api/posts/search
  - GET /api/posts/{postId}/chatrooms/count

#### 3.2 PostLikeController.java (신규 생성)
- 책임: 게시글 좋아요 및 관심목록 관리
- 엔드포인트:
  - POST /api/posts/{postId}/like
  - DELETE /api/posts/{postId}/like
  - GET /api/posts/liked
  - GET /api/posts/{postId}/like/check
- 파일 위치: src/main/java/org/ssafy/ssafymarket/controller/PostLikeController.java

#### 3.3 PostTransactionController.java (신규 생성)
- 책임: 게시글 거래 관련 기능
- 엔드포인트:
  - PATCH /api/posts/{postId}/status
  - PATCH /api/posts/{postId}/complete
  - GET /api/posts/my/selling
  - GET /api/posts/my/sold
  - GET /api/posts/my/purchased
  - GET /api/posts/my/transactions
- 파일 위치: src/main/java/org/ssafy/ssafymarket/controller/PostTransactionController.java

## 기존 구현 내역 (변경 없음)

### 인증 API
- POST /api/auth/signup: 회원가입
- POST /api/auth/login: 로그인 (FormLogin 방식, SecurityConfig에서 처리)
- POST /api/auth/logout: 로그아웃 (Spring Security에서 처리)

### 게시글 API
- 다중 이미지 업로드 (최대 10개)
- 페이징 및 정렬 (최신순, 인기순, 낮은가격, 높은가격)
- 카테고리별 조회
- 판매상태별 조회

### 채팅 API
- 채팅방 생성 또는 조회
- 채팅방 상세 조회
- 메시지 히스토리 조회
- 안읽은 메시지 개수 조회
- 전체 안읽은 메시지 개수 조회
- 채팅 이미지 업로드

### WebSocket
- STOMP 기반 실시간 채팅
- 엔드포인트: /ws
- Destination: /app/chat.send, /topic/room/{roomId}

## 데이터베이스 변경사항

없음. 기존 스키마 유지.

## 파일 변경 내역

### 신규 생성
- src/main/java/org/ssafy/ssafymarket/controller/PostLikeController.java
- src/main/java/org/ssafy/ssafymarket/controller/PostTransactionController.java

### 수정
- src/main/java/org/ssafy/ssafymarket/service/ChatRoomService.java
  - leaveChatRoom 메서드 추가
- src/main/java/org/ssafy/ssafymarket/repository/PostRepository.java
  - searchByKeyword 메서드 추가
  - searchByKeywordAndStatus 메서드 추가
- src/main/java/org/ssafy/ssafymarket/controller/PostController.java
  - 검색 API 추가 (line 976)
  - 좋아요/거래 관련 메서드는 그대로 유지 (분리 작업 미완료)

### 백업
- src/main/java/org/ssafy/ssafymarket/controller/PostController.java.backup

## 미완료 작업

1. PostController에서 좋아요 관련 메서드 제거
   - 현재 PostController와 PostLikeController에 중복 존재
   - 충돌 가능성 있음

2. PostController에서 거래 관련 메서드 제거
   - 현재 PostController와 PostTransactionController에 중복 존재
   - 충돌 가능성 있음

## 기술 스택

- Spring Boot 3.5.7
- Spring Security (폼 기반 인증, 세션 관리)
- Spring Data JPA
- MySQL (EC2: k13d201.p.ssafy.io:3307)
- MinIO (S3 호환 객체 저장소)
- WebSocket/STOMP
- Gradle

## 주의사항

1. 환경변수 설정 필요
   - DB_URL, DB_USERNAME, DB_PASSWORD
   - MINIO_URL, MINIO_APP_USER, MINIO_APP_SECRET

2. PostController 중복 엔드포인트
   - 좋아요 관련 엔드포인트가 PostController와 PostLikeController 양쪽에 존재
   - 거래 관련 엔드포인트가 PostController와 PostTransactionController 양쪽에 존재
   - 정상 동작하지만 정리 필요

3. 컨트롤러 분리 작업 완료를 위해서는 PostController에서 중복 메서드 삭제 필요
