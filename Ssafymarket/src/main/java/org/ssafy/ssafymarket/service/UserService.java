package org.ssafy.ssafymarket.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssafy.ssafymarket.dto.UserUpdateRequestDto;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.UserRepository;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;


	@Transactional
	public void updateUserInfo(String userId, UserUpdateRequestDto request) {

		//사용자 조회
		User user = userRepository.findByStudentId(userId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 입니다."));

		user.setName(request.getName());
		user.setPassword(passwordEncoder.encode(request.getPassword()));


	}
}
