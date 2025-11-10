package org.ssafy.ssafymarket.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
	private String studentId; // 학번 (로그인 ID)
	private String name;      // 이름
	private String className;     // 반
	private String password;  // 비밀번호 (평문)
}