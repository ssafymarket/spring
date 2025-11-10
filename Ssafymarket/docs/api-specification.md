# API 명세서

## 1. 인증 API

| 메서드 | 엔드포인트 | 설명 | 인증 필요 |
|--------|-----------|------|----------|
| POST | /api/auth/signup | 회원가입 | X |
| POST | /api/auth/login | 로그인 | X |
| POST | /api/auth/logout | 로그아웃 | O |

### 1.1 회원가입

**요청**
```json
POST /api/auth/signup
Content-Type: application/json

{
  "studentId": "2024001",
  "name": "홍길동",
  "className": "서울 1반",
  "password": "password123"
}
```

**응답 성공**
```json
{
  "success": true,
  "message": "회원가입 성공",
  "studentId": "2024001"
}
```

**응답 실패**
```json
{
  "success": false,
  "message": "이미 존재하는 학번입니다."
}
```

### 1.2 로그인

**요청**
```json
POST /api/auth/login
Content-Type: application/json

{
  "studentId": "2024001",
  "password": "password123"
}
```

**응답**
- 성공: HTTP 200, Set-Cookie: JSESSIONID
- 실패: HTTP 401 Unauthorized

---

## 2. 게시글 CRUD API

| 메서드 | 엔드포인트 | 설명 | 인증 필요 |
|--------|-----------|------|----------|
| POST | /api/posts | 게시글 생성 | O |
| GET | /api/posts | 게시글 목록 조회 | X |
| GET | /api/posts/{postId} | 게시글 상세 조회 | X |
| PUT | /api/posts/{postId} | 게시글 수정 | O |
| DELETE | /api/posts/{postId} | 게시글 삭제 | O |
| GET | /api/posts/category/{category} | 카테고리별 조회 | X |
| GET | /api/posts/status/{status} | 상태별 조회 | X |
| GET | /api/posts/user/{studentId} | 특정 사용자 게시글 조회 | X |
| GET | /api/posts/search | 게시글 검색 | X |
| GET | /api/posts/{postId}/chatrooms/count | 채팅방 개수 조회 | X |

### 2.1 게시글 생성

**요청**
```
POST /api/posts
Content-Type: multipart/form-data

files: 파일[] (최소 1개, 최대 10개)
title: 문자열 (필수)
price: 숫자 (필수)
category: 문자열 (필수)
description: 문자열 (선택)
```

**응답**
```json
{
  "success": true,
  "postId": 123,
  "message": "게시글이 성공적으로 생성되었습니다."
}
```

### 2.2 게시글 목록 조회

**요청**
```
GET /api/posts?page=0&size=20&sort=latest
```

**쿼리 파라미터**

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| page | integer | 0 | 페이지 번호 |
| size | integer | 20 | 페이지 크기 |
| sort | string | latest | 정렬 방식 (latest, popular, lowPrice, highPrice) |

**응답**
```json
{
  "success": true,
  "posts": [
    {
      "postId": 123,
      "title": "게시글 제목",
      "price": 50000,
      "category": "전자기기",
      "description": "상품 설명",
      "status": "판매중",
      "createdAt": "2025-11-11T10:30:00",
      "chatRoomCount": 3,
      "likeCount": 5,
      "writer": "홍길동",
      "images": ["http://minio-url/image1.jpg"]
    }
  ],
  "currentPage": 0,
  "totalPages": 5,
  "totalItems": 100,
  "pageSize": 20
}
```

### 2.3 게시글 상세 조회

**요청**
```
GET /api/posts/{postId}
```

**응답**
```json
{
  "postId": 123,
  "title": "게시글 제목",
  "price": 50000,
  "category": "전자기기",
  "description": "상품 설명",
  "status": "판매중",
  "createdAt": "2025-11-11T10:30:00",
  "chatRoomCount": 3,
  "likeCount": 5,
  "writer": {
    "studentId": "2024001",
    "name": "홍길동"
  },
  "images": ["http://minio-url/image1.jpg"]
}
```

### 2.4 게시글 수정

**요청**
```json
PUT /api/posts/{postId}
Content-Type: application/json

{
  "title": "수정된 제목",
  "price": 45000,
  "category": "전자기기",
  "description": "수정된 설명"
}
```

**응답**
```json
{
  "success": true,
  "message": "게시글이 수정되었습니다.",
  "postId": 123
}
```

### 2.5 게시글 삭제

**요청**
```
DELETE /api/posts/{postId}
```

**응답**
```json
{
  "success": true,
  "message": "게시글이 삭제되었습니다.",
  "deletedBy": "작성자"
}
```

### 2.6 게시글 검색

**요청**
```
GET /api/posts/search?keyword=노트북&status=판매중&page=0&size=20&sort=latest
```

**쿼리 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| keyword | string | O | 검색 키워드 (제목, 설명 검색) |
| status | string | X | 판매상태 (판매중, 판매완료) |
| page | integer | X | 페이지 번호 (기본: 0) |
| size | integer | X | 페이지 크기 (기본: 20) |
| sort | string | X | 정렬 방식 (기본: latest) |

**응답**
```json
{
  "success": true,
  "posts": [...],
  "currentPage": 0,
  "totalPages": 3,
  "totalItems": 45,
  "pageSize": 20,
  "keyword": "노트북"
}
```

---

## 3. 좋아요/관심목록 API

| 메서드 | 엔드포인트 | 설명 | 인증 필요 |
|--------|-----------|------|----------|
| POST | /api/posts/{postId}/like | 좋아요 추가 | O |
| DELETE | /api/posts/{postId}/like | 좋아요 취소 | O |
| GET | /api/posts/liked | 관심목록 조회 | O |
| GET | /api/posts/{postId}/like/check | 좋아요 여부 확인 | X |

### 3.1 좋아요 추가

**요청**
```
POST /api/posts/{postId}/like
```

**응답**
```json
{
  "success": true,
  "message": "좋아요를 추가했습니다.",
  "likeCount": 6
}
```

### 3.2 좋아요 취소

**요청**
```
DELETE /api/posts/{postId}/like
```

**응답**
```json
{
  "success": true,
  "message": "좋아요를 취소했습니다.",
  "likeCount": 5
}
```

### 3.3 관심목록 조회

**요청**
```
GET /api/posts/liked
```

**응답**
```json
{
  "success": true,
  "posts": [
    {
      "postId": 123,
      "title": "게시글 제목",
      "price": 50000,
      "category": "전자기기",
      "description": "상품 설명",
      "status": "판매중",
      "createdAt": "2025-11-11T10:30:00",
      "likedAt": "2025-11-11T11:00:00",
      "thumbnailUrl": "http://minio-url/image1.jpg",
      "imageUrls": [...],
      "likeCount": 5,
      "chatRoomCount": 3,
      "writer": {
        "studentId": "2024001",
        "name": "홍길동"
      }
    }
  ],
  "count": 3
}
```

### 3.4 좋아요 여부 확인

**요청**
```
GET /api/posts/{postId}/like/check
```

**응답**
```json
{
  "success": true,
  "isLiked": true
}
```

---

## 4. 거래 관리 API

| 메서드 | 엔드포인트 | 설명 | 인증 필요 |
|--------|-----------|------|----------|
| PATCH | /api/posts/{postId}/status | 판매상태 변경 | O |
| PATCH | /api/posts/{postId}/complete | 판매완료 처리 | O |
| GET | /api/posts/my/selling | 판매중인 게시글 조회 | O |
| GET | /api/posts/my/sold | 판매완료한 게시글 조회 | O |
| GET | /api/posts/my/purchased | 구매한 게시글 조회 | O |
| GET | /api/posts/my/transactions | 전체 거래내역 조회 | O |

### 4.1 판매상태 변경

**요청**
```
PATCH /api/posts/{postId}/status?status=판매중
```

**쿼리 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| status | string | O | 판매중 또는 판매완료 |

**응답**
```json
{
  "success": true,
  "message": "판매 상태가 변경되었습니다.",
  "status": "판매중"
}
```

### 4.2 판매완료 처리

**요청**
```
PATCH /api/posts/{postId}/complete?buyerId=2024002
```

**쿼리 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| buyerId | string | O | 구매자 학번 |

**응답**
```json
{
  "success": true,
  "message": "판매가 완료되었습니다.",
  "buyerId": "2024002",
  "buyerName": "김철수"
}
```

### 4.3 판매중인 게시글 조회

**요청**
```
GET /api/posts/my/selling
```

**응답**
```json
{
  "success": true,
  "posts": [...],
  "count": 5
}
```

### 4.4 판매완료한 게시글 조회

**요청**
```
GET /api/posts/my/sold
```

**응답**
```json
{
  "success": true,
  "posts": [
    {
      "postId": 123,
      "title": "게시글 제목",
      "buyerName": "김철수",
      "buyerId": "2024002",
      ...
    }
  ],
  "count": 3
}
```

### 4.5 구매한 게시글 조회

**요청**
```
GET /api/posts/my/purchased
```

**응답**
```json
{
  "success": true,
  "posts": [
    {
      "postId": 456,
      "title": "구매한 게시글",
      "sellerName": "홍길동",
      "sellerId": "2024001",
      ...
    }
  ],
  "count": 2
}
```

### 4.6 전체 거래내역 조회

**요청**
```
GET /api/posts/my/transactions
```

**응답**
```json
{
  "success": true,
  "transactions": [
    {
      "type": "판매",
      "post": {...},
      "otherParty": {
        "studentId": "2024002",
        "name": "김철수"
      },
      "completedAt": "2025-11-11T10:30:00"
    },
    {
      "type": "구매",
      "post": {...},
      "otherParty": {
        "studentId": "2024001",
        "name": "홍길동"
      },
      "completedAt": "2025-11-10T15:20:00"
    }
  ],
  "soldCount": 3,
  "purchasedCount": 2,
  "totalCount": 5
}
```

---

## 5. 채팅 API

| 메서드 | 엔드포인트 | 설명 | 인증 필요 |
|--------|-----------|------|----------|
| POST | /api/chat/room/create | 채팅방 생성 또는 조회 | O |
| GET | /api/chat/rooms | 채팅방 목록 조회 | O |
| GET | /api/chat/room/{roomId} | 채팅방 상세 조회 | O |
| GET | /api/chat/room/{roomId}/messages | 메시지 히스토리 조회 | O |
| PUT | /api/chat/room/{roomId}/read | 메시지 읽음 처리 | O |
| GET | /api/chat/room/{roomId}/unread-count | 안읽은 메시지 개수 조회 | O |
| GET | /api/chat/unread-count | 전체 안읽은 메시지 개수 조회 | O |
| DELETE | /api/chat/room/{roomId} | 채팅방 나가기 | O |
| POST | /api/chat/upload-image | 채팅 이미지 업로드 | O |

### 5.1 채팅방 생성 또는 조회

**요청**
```json
POST /api/chat/room/create
Content-Type: application/json

{
  "postId": 123
}
```

**응답**
```json
{
  "roomId": 1,
  "postId": 123,
  "postTitle": "게시글 제목",
  "postPrice": 50000,
  "postStatus": "판매중",
  "buyerId": "2024001",
  "buyerName": "홍길동",
  "sellerId": "2024002",
  "sellerName": "김철수",
  "otherPartyId": "2024002",
  "otherPartyName": "김철수",
  "lastMessage": "안녕하세요",
  "lastMessageTime": "2025-11-11T10:30:00",
  "unreadCount": 3,
  "createdAt": "2025-11-11T09:00:00"
}
```

### 5.2 채팅방 목록 조회

**요청**
```
GET /api/chat/rooms
```

**응답**
```json
[
  {
    "roomId": 1,
    "postId": 123,
    "postTitle": "게시글 제목",
    "postPrice": 50000,
    "postStatus": "판매중",
    "buyerId": "2024001",
    "buyerName": "홍길동",
    "sellerId": "2024002",
    "sellerName": "김철수",
    "otherPartyId": "2024002",
    "otherPartyName": "김철수",
    "lastMessage": "안녕하세요",
    "lastMessageTime": "2025-11-11T10:30:00",
    "unreadCount": 3,
    "createdAt": "2025-11-11T09:00:00"
  }
]
```

### 5.3 채팅방 상세 조회

**요청**
```
GET /api/chat/room/{roomId}
```

**응답**
- 5.1과 동일

### 5.4 메시지 히스토리 조회

**요청**
```
GET /api/chat/room/{roomId}/messages?page=0&size=50
```

**쿼리 파라미터**

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| page | integer | 0 | 페이지 번호 |
| size | integer | 50 | 페이지 크기 |

**응답**
```json
[
  {
    "messageId": 1,
    "roomId": 1,
    "senderId": "2024001",
    "senderName": "홍길동",
    "content": "안녕하세요",
    "messageType": "TEXT",
    "imageUrl": null,
    "isRead": true,
    "createdAt": "2025-11-11T10:30:00"
  }
]
```

### 5.5 메시지 읽음 처리

**요청**
```
PUT /api/chat/room/{roomId}/read
```

**응답**
```json
{
  "success": true,
  "message": "메시지 읽음 처리 완료"
}
```

### 5.6 안읽은 메시지 개수 조회

**요청**
```
GET /api/chat/room/{roomId}/unread-count
```

**응답**
```json
{
  "unreadCount": 3
}
```

### 5.7 전체 안읽은 메시지 개수 조회

**요청**
```
GET /api/chat/unread-count
```

**응답**
```json
{
  "totalUnreadCount": 10
}
```

### 5.8 채팅방 나가기

**요청**
```
DELETE /api/chat/room/{roomId}
```

**응답**
```json
{
  "success": true,
  "message": "채팅방을 나갔습니다."
}
```

### 5.9 채팅 이미지 업로드

**요청**
```
POST /api/chat/upload-image
Content-Type: multipart/form-data

file: 파일 (최대 10MB)
```

**응답**
```json
{
  "success": true,
  "imageUrl": "http://minio-url/chat/image.jpg",
  "message": "이미지 업로드 성공"
}
```

**사용 플로우**
1. 이 API로 이미지를 먼저 업로드
2. 서버가 MinIO에 저장 후 imageUrl 반환
3. WebSocket으로 메시지 전송: `{ messageType: "IMAGE", imageUrl: "반환받은URL", content: "" }`

---

## 6. WebSocket API

### 6.1 연결

```
CONNECT /ws
Protocol: STOMP
```

### 6.2 메시지 전송

```
SEND /app/chat.send
Content-Type: application/json
```

**메시지 형식**
```json
{
  "roomId": 1,
  "senderId": "2024001",
  "content": "안녕하세요",
  "messageType": "TEXT",
  "imageUrl": null
}
```

**메시지 타입**

| 타입 | 설명 |
|------|------|
| TEXT | 일반 텍스트 메시지 |
| IMAGE | 이미지 메시지 |

### 6.3 메시지 수신

```
SUBSCRIBE /topic/room/{roomId}
```

**수신 메시지 형식**
```json
{
  "messageId": 1,
  "roomId": 1,
  "senderId": "2024001",
  "senderName": "홍길동",
  "content": "안녕하세요",
  "messageType": "TEXT",
  "imageUrl": null,
  "isRead": false,
  "createdAt": "2025-11-11T10:30:00"
}
```

---

## 공통 사항

### 인증 방식
- 세션 기반 인증 (JSESSIONID 쿠키)
- 로그인 필요한 API는 인증되지 않은 경우 401 Unauthorized 반환

### HTTP 상태 코드

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 필요 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 오류 |

### 에러 응답 형식
```json
{
  "success": false,
  "message": "에러 메시지"
}
```

### 정렬 방식

| 값 | 설명 |
|-------|------|
| latest | 최신순 (생성일 내림차순) |
| popular | 인기순 (좋아요 수 내림차순) |
| lowPrice | 낮은 가격순 |
| highPrice | 높은 가격순 |

### 판매 상태

| 값 | 설명 |
|----|------|
| 판매중 | 판매 중인 상태 |
| 판매완료 | 판매 완료된 상태 |
