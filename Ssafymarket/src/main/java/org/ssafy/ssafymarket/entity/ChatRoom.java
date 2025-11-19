package org.ssafy.ssafymarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room",
       uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "buyer_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "post_id",
		nullable = false,
		foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
	)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", referencedColumnName = "student_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", referencedColumnName = "student_id", nullable = false)
    private User seller;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Column(name = "unread_buyer")
    @Builder.Default
    private Integer unreadBuyer = 0;

    @Column(name = "unread_seller")
    @Builder.Default
    private Integer unreadSeller = 0;

    @Column(name = "activity", nullable = false)
    @Builder.Default
    private Integer activity = 1;  // 1: 활성, 0: 비활성(삭제됨)
}
