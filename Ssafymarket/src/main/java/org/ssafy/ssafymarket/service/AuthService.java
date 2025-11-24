package org.ssafy.ssafymarket.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.ssafy.ssafymarket.dto.PasswordFindDto;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

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
		user.setPassword(passwordEncoder.encode("111111"));

		return true;


	}
}
