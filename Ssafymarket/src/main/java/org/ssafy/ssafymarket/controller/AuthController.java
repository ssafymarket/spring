package org.ssafy.ssafymarket.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
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

	/**
	 * 내 정보 조회
	 */
	@Operation(
		summary = "내 정보 조회",
		description = "로그인한 사용자의 정보 조회\n" +
			"학번, 이름, 반 정보 반환\n" +
			"인증 필요"
	)
	@GetMapping("/me")
	public ResponseEntity<Map<String, Object>> getMyInfo(Authentication authentication) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "로그인이 필요합니다."));
			}

			String studentId = authentication.getName();
			User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			return ResponseEntity.ok(Map.of(
				"success", true,
				"user", Map.of(
					"studentId", user.getStudentId(),
					"name", user.getName(),
					"className", user.getClassName(),
					"role", user.getRole().name()
				)
			));

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

}
