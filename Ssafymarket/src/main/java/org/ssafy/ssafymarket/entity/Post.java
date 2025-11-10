package org.ssafy.ssafymarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "chat_room_count")
    @Builder.Default
    private Integer chatRoomCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PostStatus status = PostStatus.판매중;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", referencedColumnName = "student_id")
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", referencedColumnName = "student_id")
    private User buyer;

    /**
     * @deprecated 다중 이미지 지원을 위해 images 리스트 사용
     * 기존 데이터 마이그레이션 후 삭제 예정
     */
    @Deprecated
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * 게시글의 이미지 목록 (순서대로 정렬)
     * - CascadeType.ALL: Post 삭제 시 이미지도 함께 삭제
     * - orphanRemoval: 리스트에서 제거된 이미지 자동 삭제
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("imageOrder ASC")
    @Builder.Default
    private List<PostImage> images = new ArrayList<>();

    /**
     * 대표 이미지 URL 반환 (첫 번째 이미지)
     * @return 대표 이미지 URL, 없으면 null
     */
    public String getThumbnailUrl() {
        if (!images.isEmpty()) {
            return images.get(0).getImageUrl();
        }
        // Fallback: 기존 imageUrl 사용 (마이그레이션 기간 동안)
        return imageUrl;
    }

    /**
     * 모든 이미지 URL 리스트 반환
     * @return 이미지 URL 리스트
     */
    public List<String> getImageUrls() {
        return images.stream()
                .map(PostImage::getImageUrl)
                .toList();
    }

    /**
     * 이미지 추가 편의 메서드
     * @param imageUrl MinIO 이미지 URL
     */
    public void addImage(String imageUrl) {
        PostImage postImage = PostImage.builder()
                .post(this)
                .imageUrl(imageUrl)
                .imageOrder(images.size())  // 현재 크기가 곧 다음 순서
                .build();
        images.add(postImage);
    }

    public enum PostStatus {
        판매중,
        판매완료
    }
}
