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

    // 특정 게시글과 구매자로 채팅방 조회 (중복 생성 방지) - Fetch Join으로 성능 최적화
    @Query("SELECT cr FROM ChatRoom cr " +
           "JOIN FETCH cr.post p " +
           "JOIN FETCH cr.buyer b " +
           "JOIN FETCH cr.seller s " +
           "JOIN FETCH p.writer " +
           "WHERE p.postId = :postId AND b.studentId = :buyerId")
    Optional<ChatRoom> findByPost_PostIdAndBuyer_StudentId(@Param("postId") Long postId, @Param("buyerId") String buyerId);

    // 사용자가 참여한 모든 채팅방 조회 - Fetch Join으로 성능 최적화
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "JOIN FETCH cr.post p " +
           "JOIN FETCH cr.buyer b " +
           "JOIN FETCH cr.seller s " +
           "JOIN FETCH p.writer " +
           "WHERE (cr.buyer.studentId = :userId OR cr.seller.studentId = :userId) AND cr.activity = 1 " +
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

    // ID로 채팅방 조회 - Fetch Join으로 성능 최적화
    @Query("SELECT cr FROM ChatRoom cr " +
           "JOIN FETCH cr.post p " +
           "JOIN FETCH cr.buyer b " +
           "JOIN FETCH cr.seller s " +
           "JOIN FETCH p.writer " +
           "WHERE cr.roomId = :roomId AND cr.activity = 1")
    Optional<ChatRoom> findByIdWithFetch(@Param("roomId") Long roomId);
}
