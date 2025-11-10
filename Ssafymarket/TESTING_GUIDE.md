# 채팅 기능 테스트 가이드

## 사전 준비

### 1. 데이터베이스 설정

환경 변수 설정:
```bash
set DB_URL=jdbc:mysql://localhost:3306/ssafymarket?useSSL=false&serverTimezone=Asia/Seoul
set DB_USERNAME=your_username
set DB_PASSWORD=your_password
```

또는 Windows PowerShell:
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ssafymarket?useSSL=false&serverTimezone=Asia/Seoul"
$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"
```

### 2. 애플리케이션 빌드

```bash
gradlew build
```

### 3. 비밀번호 해시 생성

테스트 사용자의 비밀번호를 암호화하기 위해:

```bash
gradlew run --args="org.ssafy.ssafymarket.util.PasswordEncoderUtil"
```

또는 IntelliJ에서 `PasswordEncoderUtil.java` 파일을 직접 실행

출력된 해시값을 복사합니다.

### 4. 테스트 데이터 삽입

MySQL에 접속:
```bash
mysql -u your_username -p ssafymarket
```

비밀번호 해시를 사용하여 사용자 생성:
```sql
-- 3단계에서 생성한 해시값으로 교체
INSERT INTO user (student_id, name, class, password, role) VALUES
('2024001', '김철수', '1반', '여기에_해시값_입력', 'ROLE_USER'),
('2024002', '이영희', '2반', '여기에_해시값_입력', 'ROLE_USER'),
('2024003', '박민수', '1반', '여기에_해시값_입력', 'ROLE_USER');

-- 게시글 생성
INSERT INTO post (title, price, category, chat_room_count, like_count, status, writer_id, image_url) VALUES
('아이패드 프로 11인치 팝니다', 500000, '전자기기', 0, 0, '판매중', '2024001', NULL),
('맥북 에어 M1 판매', 800000, '전자기기', 0, 0, '판매중', '2024001', NULL),
('자전거 팝니다', 150000, '스포츠', 0, 0, '판매중', '2024002', NULL);
```

---

## 애플리케이션 실행

```bash
gradlew bootRun
```

또는 IntelliJ에서 `SsafymarketApplication` 실행

서버가 시작되면 콘솔에 다음과 같은 로그가 표시됩니다:
```
Started SsafymarketApplication in X.XXX seconds
```

---

## 테스트 방법

### 방법 1: 웹 브라우저 테스트 페이지 사용 (권장)

1. 브라우저에서 접속:
   ```
   http://localhost:8080/chat-test.html
   ```

2. 테스트 순서:
   - 1단계: 로그인
     - 학번: 2024001
     - 비밀번호: password123
     - "로그인" 버튼 클릭

   - 2단계: 채팅방 생성
     - 게시글 ID: 1
     - "채팅방 생성" 버튼 클릭

   - 3단계: WebSocket 연결
     - "WebSocket 연결" 버튼 클릭

   - 4단계: 메시지 전송
     - 메시지 입력 후 "전송" 버튼 클릭

   - 5단계: 채팅방 목록 확인
     - "목록 조회" 버튼 클릭

3. 2명 테스트 방법:
   - 크롬 일반 창: 2024001로 로그인
   - 크롬 시크릿 창: 2024002로 로그인
   - 2024002가 게시글 ID 1번으로 채팅방 생성 (2024001의 게시글)
   - 양쪽에서 WebSocket 연결 후 메시지 주고받기

---

### 방법 2: Postman 사용

#### 1. 로그인
```
POST http://localhost:8080/api/auth/login
Content-Type: application/x-www-form-urlencoded

studentId=2024001&password=password123
```

쿠키가 자동으로 저장됩니다 (JSESSIONID).

#### 2. 채팅방 생성
```
POST http://localhost:8080/api/chat/room/create
Content-Type: application/json

{
    "postId": 1
}
```

응답 예시:
```json
{
    "roomId": 1,
    "postId": 1,
    "postTitle": "아이패드 프로 11인치 팝니다",
    "postPrice": 500000,
    "buyerId": "2024002",
    "sellerId": "2024001",
    "unreadCount": 0
}
```

#### 3. 내 채팅방 목록 조회
```
GET http://localhost:8080/api/chat/rooms
```

#### 4. 채팅방 메시지 조회
```
GET http://localhost:8080/api/chat/room/1/messages?page=0&size=50
```

#### 5. 안읽은 메시지 수 조회
```
GET http://localhost:8080/api/chat/unread-count
```

---

### 방법 3: curl 사용

#### 로그인
```bash
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "studentId=2024001&password=password123"
```

#### 채팅방 생성
```bash
curl -b cookies.txt -X POST http://localhost:8080/api/chat/room/create \
  -H "Content-Type: application/json" \
  -d '{"postId": 1}'
```

#### 채팅방 목록 조회
```bash
curl -b cookies.txt http://localhost:8080/api/chat/rooms
```

---

## WebSocket 테스트 (개발자 도구)

브라우저 개발자 도구 Console에서:

```javascript
// SockJS와 STOMP 라이브러리 로드 (chat-test.html에서 이미 로드됨)

// WebSocket 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // 채팅방 구독 (roomId는 실제 값으로 변경)
    stompClient.subscribe('/topic/room/1', function(message) {
        console.log('Received:', JSON.parse(message.body));
    });

    // 메시지 전송
    stompClient.send('/app/chat/send/1', {}, JSON.stringify({
        content: 'Hello!',
        messageType: 'CHAT'
    }));
});
```

---

## 예상 테스트 결과

### 성공 케이스

1. 로그인 성공:
   - 응답: {"success": true, "message": "로그인 성공"}
   - HTTP 200 OK

2. 채팅방 생성 성공:
   - roomId, postTitle 등 정보 반환
   - HTTP 200 OK

3. 메시지 전송 성공:
   - 실시간으로 구독자에게 메시지 전달
   - DB에 메시지 저장됨

4. 읽음 처리 성공:
   - 안읽은 메시지 카운트 0으로 변경
   - is_read = true로 업데이트

### 실패 케이스

1. 로그인 실패:
   - 잘못된 학번/비밀번호: HTTP 401, {"success": false}

2. 인증 없이 API 호출:
   - HTTP 403 Forbidden

3. 본인 게시글에 채팅 시도:
   - "본인 게시글에는 채팅할 수 없습니다" 에러

4. WebSocket 연결 실패:
   - 로그인하지 않은 경우: 연결 거부

---

## 데이터베이스 확인

테스트 후 MySQL에서 데이터 확인:

```sql
-- 채팅방 확인
SELECT * FROM chat_room;

-- 메시지 확인
SELECT
    cm.message_id,
    cm.sender_id,
    cm.content,
    cm.sent_at,
    cm.is_read
FROM chat_message cm
ORDER BY cm.sent_at DESC
LIMIT 10;

-- 안읽은 메시지 확인
SELECT
    cr.room_id,
    cr.buyer_id,
    cr.seller_id,
    cr.unread_buyer,
    cr.unread_seller
FROM chat_room cr;

-- 세션 테이블 확인
SELECT * FROM SPRING_SESSION;
```

---

## 문제 해결

### 1. 로그인이 안 되는 경우

원인: 비밀번호가 암호화되지 않았거나 잘못된 해시
해결: PasswordEncoderUtil로 새 해시 생성 후 DB 업데이트

```sql
UPDATE user SET password = '새로운_해시값' WHERE student_id = '2024001';
```

### 2. WebSocket 연결이 안 되는 경우

원인: 로그인하지 않았거나 세션이 만료됨
해결: 다시 로그인 후 WebSocket 연결

### 3. CORS 에러가 발생하는 경우

원인: 다른 포트나 도메인에서 접근
해결: WebSocketConfig에서 setAllowedOriginPatterns("*")로 설정됨 (이미 적용)

### 4. 메시지가 전송되지 않는 경우

확인 사항:
- WebSocket이 연결되어 있는지 확인
- 채팅방에 권한이 있는지 확인 (buyer 또는 seller)
- 브라우저 콘솔에서 에러 메시지 확인

### 5. 데이터베이스 연결 오류

원인: 환경 변수 미설정 또는 MySQL 서버 미실행
해결:
- MySQL 서버 실행 확인
- 환경 변수 재설정
- DB_URL, DB_USERNAME, DB_PASSWORD 확인

---

## 로그 확인

application.yml에서 DEBUG 로그가 활성화되어 있습니다.

콘솔에서 확인할 수 있는 로그:
- WebSocket 연결/해제: "사용자 연결 - sessionId: xxx"
- 메시지 저장: "메시지 저장 - roomId: xxx"
- 읽음 처리: "메시지 읽음 처리 - roomId: xxx"
- 채팅방 생성: "새 채팅방 생성 - roomId: xxx"

로그 파일 위치: 콘솔 출력 (파일 로깅은 미설정)

---

## 테스트 체크리스트

- [ ] 사용자 로그인 성공
- [ ] 채팅방 생성 성공
- [ ] WebSocket 연결 성공
- [ ] 메시지 전송 및 수신 확인
- [ ] 메시지가 DB에 저장되는지 확인
- [ ] 안읽은 메시지 카운트 증가 확인
- [ ] 읽음 처리 후 카운트 감소 확인
- [ ] 상대방에게 읽음 알림 전송 확인
- [ ] 채팅방 목록 조회 성공
- [ ] 메시지 히스토리 조회 성공
- [ ] 2명의 사용자 간 실시간 채팅 확인
- [ ] WebSocket 연결 해제 시 정상 처리
- [ ] 로그아웃 후 재로그인 시 채팅 히스토리 유지 확인

---

## 추가 테스트 시나리오

### 시나리오 1: 기본 1:1 채팅
1. 사용자 2024001 로그인 (판매자)
2. 사용자 2024002 로그인 (구매자, 다른 브라우저)
3. 2024002가 2024001의 게시글(ID:1)에 채팅방 생성
4. 양쪽 모두 WebSocket 연결
5. 메시지 주고받기 확인

### 시나리오 2: 읽음 처리
1. 2024002가 메시지 전송
2. 2024001의 안읽은 메시지 카운트 확인 (1개)
3. 2024001이 채팅방 입장 후 읽음 처리
4. 안읽은 메시지 카운트 확인 (0개)
5. 2024002에게 읽음 알림 도착 확인

### 시나리오 3: 여러 채팅방
1. 2024002가 게시글 1번에 채팅방 생성
2. 2024003이 게시글 1번에 채팅방 생성
3. 2024001의 채팅방 목록에 2개 채팅방 표시 확인
4. 각 채팅방에서 독립적으로 메시지 전송 확인

### 시나리오 4: 세션 유지
1. 로그인 후 채팅
2. 브라우저 새로고침
3. 세션이 유지되는지 확인
4. 채팅 히스토리가 남아있는지 확인

---

## 성능 테스트 (선택)

동시 접속자 테스트는 JMeter 또는 Apache Bench 사용 권장.

간단한 부하 테스트:
- 10명의 사용자 동시 로그인
- 각 사용자가 1개의 채팅방 생성
- 각 채팅방에서 10개 메시지 전송
- 모든 메시지가 정상 전달되는지 확인
