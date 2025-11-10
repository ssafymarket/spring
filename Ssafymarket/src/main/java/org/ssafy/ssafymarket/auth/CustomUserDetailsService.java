package org.ssafy.ssafymarket.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String studentId) throws UsernameNotFoundException {
		User user = userRepository.findByStudentId(studentId)
			.orElseThrow(()->new UsernameNotFoundException("Not found : "+studentId));

		return org.springframework.security.core.userdetails.User
			.withUsername(user.getStudentId())
			.password(user.getPassword())
			.authorities(user.getRole().name()) // ROLE_USER or ROLE_ADMIN
			.build();

	}
}
