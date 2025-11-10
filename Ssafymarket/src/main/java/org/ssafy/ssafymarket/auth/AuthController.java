package org.ssafy.ssafymarket.auth;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class AuthController {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody SignupRequest request){
		//중복체크
		if (userRepository.existsById(request.getStudentId())) {
			return ResponseEntity
				.badRequest()
				.body(Map.of("success", false, "message", "이미 존재하는 학번입니다."));
		}

		User user = new User();
		user.setStudentId(request.getStudentId());
		user.setName(request.getName());
		user.setClassName(request.getClassName());
		user.setPassword(passwordEncoder.encode(request.getPassword())); // 암호화
		user.setRole(User.UserRole.ROLE_USER);


		userRepository.save(user);

		return ResponseEntity.ok(Map.of(
			"success", true,
			"message", "회원가입 성공",
			"studentId", user.getStudentId()

		));

	}

}
