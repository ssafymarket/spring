package org.ssafy.ssafymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssafy.ssafymarket.entity.Trade;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    // 특정 게시글의 거래 내역 조회
    Optional<Trade> findByPost_PostId(Long postId);

    // 구매자의 거래 내역 조회
    List<Trade> findByBuyer_StudentIdOrderByAgreedAtDesc(String buyerId);

    // 판매자의 거래 내역 조회
    List<Trade> findBySeller_StudentIdOrderByAgreedAtDesc(String sellerId);

    // 특정 게시글에 거래가 있는지 확인
    boolean existsByPost_PostId(Long postId);
}
