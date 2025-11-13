package org.ssafy.ssafymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssafy.ssafymarket.entity.TempUser;

import java.util.List;

@Repository
public interface TempUserRepository extends JpaRepository<TempUser,String>{
    List<TempUser> findByApprove(int approve);
}

