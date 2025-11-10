package org.ssafy.ssafymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssafy.ssafymarket.entity.ChatRoom;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 특정 게시글과 구매자로 채팅방 조회 (중복 생성 방지)
    Optional<ChatRoom> findByPost_PostIdAndBuyer_StudentId(Long postId, String buyerId);

    // 사용자가 참여한 모든 채팅방 조회
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.buyer.studentId = :userId OR cr.seller.studentId = :userId " +
           "ORDER BY cr.lastMessageTime DESC NULLS LAST, cr.createdAt DESC")
    List<ChatRoom> findAllByUserId(@Param("userId") String userId);

    // 구매자로 참여한 채팅방 조회
    List<ChatRoom> findByBuyer_StudentIdOrderByLastMessageTimeDesc(String buyerId);

    // 판매자로 참여한 채팅방 조회
    List<ChatRoom> findBySeller_StudentIdOrderByLastMessageTimeDesc(String sellerId);

    // 특정 게시글의 모든 채팅방 조회
    List<ChatRoom> findByPost_PostId(Long postId);

    // 특정 게시글의 채팅방 개수
    long countByPost_PostId(Long postId);
}
