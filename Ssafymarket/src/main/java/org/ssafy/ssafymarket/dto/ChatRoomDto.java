package org.ssafy.ssafymarket.dto;

import lombok.*;
import org.ssafy.ssafymarket.entity.ChatRoom;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {

    private Long roomId;
    private Long postId;
    private String postTitle;
    private String postImage;
    private Integer postPrice;
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String sellerName;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Boolean iAmBuyer;
    private LocalDateTime createdAt;

    // Entity to DTO (현재 사용자 기준)
    public static ChatRoomDto fromEntity(ChatRoom chatRoom, String currentUserId) {
        boolean iAmBuyer = chatRoom.getBuyer().getStudentId().equals(currentUserId);

        return ChatRoomDto.builder()
                .roomId(chatRoom.getRoomId())
                .postId(chatRoom.getPost().getPostId())
                .postTitle(chatRoom.getPost().getTitle())
                .postImage(chatRoom.getPost().getImageUrl())
                .postPrice(chatRoom.getPost().getPrice())
                .buyerId(chatRoom.getBuyer().getStudentId())
                .buyerName(chatRoom.getBuyer().getName())
                .sellerId(chatRoom.getSeller().getStudentId())
                .sellerName(chatRoom.getSeller().getName())
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageTime(chatRoom.getLastMessageTime())
                .unreadCount(iAmBuyer ? chatRoom.getUnreadBuyer() : chatRoom.getUnreadSeller())
                .iAmBuyer(iAmBuyer)
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
