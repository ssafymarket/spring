package org.ssafy.ssafymarket.dto;

import lombok.Getter;
import lombok.Setter;
import org.ssafy.ssafymarket.entity.User;

@Getter
@Setter
public class SignupRequest {
	private String studentId; // 학번 (로그인 ID)
	private String name;      // 이름
	private String className;     // 반
	private String password;  // 비밀번호 (평문)
	private User.Campus campus;   // 캠퍼스 (SEOUL, BUULGYEONG, DAEJEON, GUMI, GWANGJU)
}