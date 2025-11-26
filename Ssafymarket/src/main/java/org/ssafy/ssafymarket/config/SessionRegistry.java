package org.ssafy.ssafymarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

@Configuration
public class SessionRegistry {

	@Bean
	public SpringSessionBackedSessionRegistry<? extends Session> springSessionBackedSessionRegistry(
		FindByIndexNameSessionRepository<? extends Session> sessionRepository){
		return new SpringSessionBackedSessionRegistry<>(sessionRepository);
	}
}