package org.ssafy.ssafymarket.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssafy.ssafymarket.entity.ChatMessage;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 채팅방의 메시지 조회 (최신순)
    List<ChatMessage> findByChatRoom_RoomIdOrderBySentAtDesc(Long roomId);

    // 특정 채팅방의 메시지 조회 (페이징)
    List<ChatMessage> findByChatRoom_RoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);

    // 안읽은 메시지 개수 조회
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.chatRoom.roomId = :roomId " +
           "AND m.sender.studentId != :userId " +
           "AND m.isRead = false")
    long countUnreadMessages(@Param("roomId") Long roomId, @Param("userId") String userId);

    // 특정 채팅방의 안읽은 메시지를 읽음 처리
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP " +
           "WHERE m.chatRoom.roomId = :roomId " +
           "AND m.sender.studentId != :userId " +
           "AND m.isRead = false")
    int markMessagesAsRead(@Param("roomId") Long roomId, @Param("userId") String userId);

    // 특정 채팅방의 마지막 메시지 조회
    ChatMessage findTopByChatRoom_RoomIdOrderBySentAtDesc(Long roomId);

    // 특정 사용자의 전체 안읽은 메시지 개수
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "JOIN m.chatRoom cr " +
           "WHERE (cr.buyer.studentId = :userId OR cr.seller.studentId = :userId) " +
           "AND m.sender.studentId != :userId " +
           "AND m.isRead = false")
    long countTotalUnreadMessages(@Param("userId") String userId);
}
