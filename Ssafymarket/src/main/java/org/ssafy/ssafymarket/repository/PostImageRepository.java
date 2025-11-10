package org.ssafy.ssafymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssafy.ssafymarket.entity.PostImage;

import java.util.List;

/**
 * PostImage 엔티티에 대한 데이터 접근 레이어
 */
@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    /**
     * 특정 게시글의 이미지 목록 조회 (순서대로)
     * @param postId 게시글 ID
     * @return 이미지 목록 (imageOrder 순서대로 정렬)
     */
    List<PostImage> findByPost_PostIdOrderByImageOrderAsc(Long postId);

    /**
     * 특정 게시글의 이미지 개수 조회
     * @param postId 게시글 ID
     * @return 이미지 개수
     */
    int countByPost_PostId(Long postId);

    /**
     * 특정 게시글의 모든 이미지 삭제
     * @param postId 게시글 ID
     */
    void deleteByPost_PostId(Long postId);
}
