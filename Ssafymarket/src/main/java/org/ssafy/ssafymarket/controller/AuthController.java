package org.ssafy.ssafymarket.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssafy.ssafymarket.dto.LoginRequest;
import org.ssafy.ssafymarket.dto.SignupRequest;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

@Tag(name = "회원관리", description = "회원관리 API")
public class AuthController {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;


	@Operation(summary = "로그인 (세션 발급)",
		description = "studentId / password JSON으로 로그인하면 Set-Cookie: SESSION=... 발급")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		required = true,
		content = @Content(schema = @Schema(implementation = LoginRequest.class))
	)
	@PostMapping("/login")
	public void loginDocOnly() {
		// 문서만을 위한 stub. 실제 처리는 JsonUsernamePasswordAuthFilter가 수행.
	}


	@Operation(
		summary = "회원가입",
		description = "학번 ,이름,반,비밀 번호를 입력하여 회원가입을 진행한다."
	)
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
