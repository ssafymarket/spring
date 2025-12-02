package org.ssafy.ssafymarket.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.ssafy.ssafymarket.dto.PasswordFindDto;
import org.ssafy.ssafymarket.dto.SignupRequest;
import org.ssafy.ssafymarket.entity.TempUser;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.TempUserRepository;
import org.ssafy.ssafymarket.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TempUserRepository tempUserRepository;

	@Transactional
	public ResponseEntity<?> signup(SignupRequest request){
		//중복체크
		if (userRepository.existsById(request.getStudentId())) {
			return ResponseEntity
				.badRequest()
				.body(Map.of("success", false, "message", "이미 존재하는 학번입니다."));
		}

		//임시 회원등록에드 중복 체크 확인
		if(tempUserRepository.existsById(request.getStudentId())){
			return ResponseEntity
				.badRequest()
				.body(Map.of("success", false, "message", "이미 등록 하셨습니다."));
		}


		TempUser user = new TempUser();
		user.setStudentId(request.getStudentId());
		user.setName(request.getName());
		user.setClassName(request.getClassName());
		user.setPassword(passwordEncoder.encode(request.getPassword())); // 암호화
		user.setCampus(request.getCampus()); // 캠퍼스 설정

		tempUserRepository.save(user);

		return ResponseEntity.ok(Map.of(
			"success", true,
			"message", "회원가입 성공",
			"studentId", user.getStudentId()

		));
	}
	@Transactional
	public Boolean passwordFind(PasswordFindDto findDto){
		String studentId=findDto.getStudentId();
		String name=findDto.getName();
		String className=findDto.getClassName();

		Optional<User> optionalUser =
			userRepository.findByStudentIdAndNameAndClassName(studentId, name, className);

		if (optionalUser.isEmpty()) {
			return false; // 존재하지 않음
		}
		User user=optionalUser.get();
		user.setPassword(passwordEncoder.encode("123456"));

		return true;


	}

	public Map<String, Object> getMyInfo(String studentId) {

		User user = userRepository.findByStudentId(studentId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		return Map.of(
			"success", true,
			"user", Map.of(
				"studentId", user.getStudentId(),
				"name", user.getName(),
				"className", user.getClassName(),
				"role", user.getRole().name()
			)
		);
	}
}
