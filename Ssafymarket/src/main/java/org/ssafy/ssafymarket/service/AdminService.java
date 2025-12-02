package org.ssafy.ssafymarket.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.ssafy.ssafymarket.dto.SignupRequest;
import org.ssafy.ssafymarket.entity.TempUser;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.TempUserRepository;
import org.ssafy.ssafymarket.repository.UserRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final TempUserRepository tempUserRepository;
    private final UserRepository userRepository;

    public List<TempUser> findAll() {
        return tempUserRepository.findByApprove(0);
    }


    @Transactional
    public User updateStatus(String id) {
        TempUser tempUser = tempUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 TempUser ID입니다. id=" + id));

        tempUser.setApprove(1);

        User user=User.builder()
                .studentId(tempUser.getStudentId())
                .name(tempUser.getName())
                .className(tempUser.getClassName())
                .password(tempUser.getPassword())
                .campus(tempUser.getCampus())
                .role(User.UserRole.ROLE_USER)
                .build();

        return userRepository.save(user);

    }

	@Transactional
	public void rejectApproval(String id) {
		TempUser tempUser = tempUserRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 TempUser ID입니다. id=" + id));

		tempUserRepository.delete(tempUser);


	}


}
