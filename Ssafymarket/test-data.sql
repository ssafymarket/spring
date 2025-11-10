-- 테스트 데이터 삽입 스크립트

-- 1. 테스트 사용자 생성 (비밀번호: password123)
-- BCrypt 해시값: $2a$10$xN9P.Xtx3Q4YJ0YvY9YJ7OZvqHvYq3Y8kQ3qZ8xQ3Y8xQ3Y8xQ3Y8u
INSERT INTO user (student_id, name, class, password, role) VALUES
('2024001', '김철수', '1반', '$2a$10$xN9P.Xtx3Q4YJ0YvY9YJ7OZvqHvYq3Y8kQ3qZ8xQ3Y8xQ3Y8xQ3Y8u', 'ROLE_USER'),
('2024002', '이영희', '2반', '$2a$10$xN9P.Xtx3Q4YJ0YvY9YJ7OZvqHvYq3Y8kQ3qZ8xQ3Y8xQ3Y8xQ3Y8u', 'ROLE_USER'),
('2024003', '박민수', '1반', '$2a$10$xN9P.Xtx3Q4YJ0YvY9YJ7OZvqHvYq3Y8kQ3qZ8xQ3Y8xQ3Y8xQ3Y8u', 'ROLE_USER');

-- 2. 테스트 게시글 생성
INSERT INTO post (title, price, category, chat_room_count, like_count, status, writer_id, image_url) VALUES
('아이패드 프로 11인치 팝니다', 500000, '전자기기', 0, 0, '판매중', '2024001', 'https://example.com/ipad.jpg'),
('맥북 에어 M1 판매', 800000, '전자기기', 0, 0, '판매중', '2024001', 'https://example.com/macbook.jpg'),
('자전거 팝니다', 150000, '스포츠', 0, 0, '판매중', '2024002', 'https://example.com/bike.jpg');

-- 참고: 비밀번호는 모두 'password123' 입니다.
