package org.ssafy.ssafymarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "post_image",
    indexes = {
        @Index(name = "idx_post_order", columnList = "post_id, image_order")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /**
     * 이미지 순서 (0부터 시작, 0이 대표 이미지)
     * - 0: 썸네일로 사용되는 대표 이미지
     * - 1~: 추가 이미지들
     */
    @Column(name = "image_order", nullable = false)
    private Integer imageOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Post와의 양방향 관계 편의 메서드
     */
    public void setPost(Post post) {
        // 기존 관계 제거
        if (this.post != null) {
            this.post.getImages().remove(this);
        }

        this.post = post;

        // 새로운 관계 설정
        if (post != null && !post.getImages().contains(this)) {
            post.getImages().add(this);
        }
    }
}
