package org.ssafy.ssafymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssafy.ssafymarket.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
}
