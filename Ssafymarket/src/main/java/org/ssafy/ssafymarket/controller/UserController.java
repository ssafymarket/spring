package org.ssafy.ssafymarket.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssafy.ssafymarket.dto.UserUpdateRequestDto;
import org.ssafy.ssafymarket.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Slf4j
@Tag(name = "유저", description = "유저 정보 상태변경 API")
public class UserController {

	private final UserService userService;


	/**
	 * 유저 정보 업데이트
	 * 비번수정
	 * 이름 수정
	 * 	private String name;
	 * 	private String password;
	 * */
	@Operation(
		summary = "유저 정보 업데이트",
		description = "name,password 기반으로 정보 수정 "
	)
	@PutMapping("/update")
	public ResponseEntity<Map<String, Object>> updateUserProfile(Authentication authentication, @RequestBody UserUpdateRequestDto request){

		String userId = authentication.getName();
		userService.updateUserInfo(userId, request);
		try {
			Map<String, Object> body = new HashMap<>();
			return ResponseEntity.ok(Map.of("success", true, "message", "회원정보가 성공적으로 수정되었습니다."));
		}catch (IllegalArgumentException e){
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("success", false, "message", e.getMessage()));
		}catch (Exception e) {
			// 500: 기타
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
		}

	}






}
