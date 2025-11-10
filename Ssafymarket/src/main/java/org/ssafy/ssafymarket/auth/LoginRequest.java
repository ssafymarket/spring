package org.ssafy.ssafymarket.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {
	private String studentId;
	private String password;
}
