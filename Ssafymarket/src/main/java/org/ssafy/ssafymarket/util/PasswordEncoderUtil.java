package org.ssafy.ssafymarket.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String password = "password123";
        String encoded = encoder.encode(password);

        System.out.println("원본 비밀번호: " + password);
        System.out.println("인코딩된 비밀번호: " + encoded);
        System.out.println("\n다음 SQL을 실행하세요:");
        System.out.println("UPDATE user SET password = '" + encoded + "' WHERE student_id = '2024001';");
    }
}
