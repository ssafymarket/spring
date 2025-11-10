-- 테스트 사용자 및 게시글 데이터
-- 모든 사용자의 비밀번호: password123
-- BCrypt 해시: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy

-- 1. 사용자 생성
INSERT INTO user (student_id, name, class, password, role) VALUES
('2024001', '김철수', '1반', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy', 'ROLE_USER'),
('2024002', '이영희', '2반', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy', 'ROLE_USER'),
('2024003', '박민수', '1반', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy', 'ROLE_USER');

-- 2. 게시글 생성
INSERT INTO post (title, price, category, chat_room_count, like_count, status, writer_id, image_url, created_at) VALUES
('아이패드 프로 11인치 팝니다', 500000, '전자기기', 0, 0, '판매중', '2024001', NULL, NOW()),
('맥북 에어 M1 판매', 800000, '전자기기', 0, 0, '판매중', '2024001', NULL, NOW()),
('자전거 팝니다', 150000, '스포츠', 0, 0, '판매중', '2024002', NULL, NOW()),
('책상 팝니다', 50000, '가구', 0, 0, '판매중', '2024003', NULL, NOW());

-- 3. 확인
SELECT student_id, name, class, role FROM user;
SELECT post_id, title, price, status, writer_id FROM post;
