package org.ssafy.ssafymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssafy.ssafymarket.entity.PostLike;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PostLikeId> {
    Optional<PostLike> findByUserIdAndPostId(String userId, Long postId);
    List<PostLike> findByUserId(String userId);
    List<PostLike> findByPostId(Long postId);
    long countByPostId(Long postId);
    boolean existsByUserIdAndPostId(String userId, Long postId);
}
