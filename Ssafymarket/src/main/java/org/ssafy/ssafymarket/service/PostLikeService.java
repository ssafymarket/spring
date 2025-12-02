package org.ssafy.ssafymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssafy.ssafymarket.entity.Post;
import org.ssafy.ssafymarket.entity.PostLike;
import org.ssafy.ssafymarket.repository.PostLikeRepository;
import org.ssafy.ssafymarket.repository.PostRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostLikeService {

	private final PostRepository postRepository;
	private final PostLikeRepository postLikeRepository;

	/* ===================== 좋아요 추가 ===================== */

	@Transactional
	public Map<String, Object> addLike(Long postId, String studentId) {

		if (!postRepository.existsById(postId)) {
			throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
		}

		if (postLikeRepository.existsByUserIdAndPostId(studentId, postId)) {
			throw new IllegalStateException("이미 좋아요한 게시글입니다.");
		}

		PostLike postLike = PostLike.builder()
			.userId(studentId)
			.postId(postId)
			.build();

		postLikeRepository.save(postLike);

		long likeCount = postLikeRepository.countByPostId(postId);

		log.info("좋아요 추가 - postId: {}, userId: {}, likeCount: {}", postId, studentId, likeCount);

		return Map.of(
			"success", true,
			"message", "좋아요를 추가했습니다.",
			"likeCount", likeCount
		);
	}

	/* ===================== 좋아요 취소 ===================== */

	@Transactional
	public Map<String, Object> removeLike(Long postId, String studentId) {

		PostLike postLike = postLikeRepository.findByUserIdAndPostId(studentId, postId)
			.orElseThrow(() -> new IllegalArgumentException("좋아요하지 않은 게시글입니다."));

		postLikeRepository.delete(postLike);

		long likeCount = postLikeRepository.countByPostId(postId);

		log.info("좋아요 취소 - postId: {}, userId: {}, likeCount: {}", postId, studentId, likeCount);

		return Map.of(
			"success", true,
			"message", "좋아요를 취소했습니다.",
			"likeCount", likeCount
		);
	}

	/* ===================== 내가 좋아요한 게시글 목록 ===================== */

	public Map<String, Object> getLikedPosts(String studentId) {

		List<PostLike> likes = postLikeRepository.findByUserId(studentId);

		List<Map<String, Object>> postList = likes.stream()
			.map(like -> {
				Post post = like.getPost();
				return convertPostToMap(post, like);
			})
			.collect(Collectors.toList());

		log.info("관심목록 조회 - userId: {}, count: {}", studentId, postList.size());

		return Map.of(
			"success", true,
			"posts", postList,
			"count", postList.size()
		);
	}

	/* ===================== 좋아요 여부 확인 ===================== */

	public boolean isLiked(Long postId, String studentId) {
		return postLikeRepository.existsByUserIdAndPostId(studentId, postId);
	}

	/* ===================== 헬퍼 ===================== */

	private Map<String, Object> convertPostToMap(Post post, PostLike like) {
		Map<String, Object> map = new HashMap<>();
		map.put("postId", post.getPostId());
		map.put("title", post.getTitle());
		map.put("price", post.getPrice());
		map.put("category", post.getCategory() != null ? post.getCategory() : "");
		map.put("description", post.getDescription() != null ? post.getDescription() : "");
		map.put("status", post.getStatus());
		map.put("createdAt", post.getCreatedAt());
		map.put("likedAt", like.getLikedAt());
		map.put("thumbnailUrl", post.getThumbnailUrl() != null ? post.getThumbnailUrl() : "");
		map.put("imageUrls", post.getImageUrls());
		map.put("likeCount", postLikeRepository.countByPostId(post.getPostId()));
		map.put("chatRoomCount", post.getChatRoomCount());
		map.put("writer", Map.of(
			"studentId", post.getWriter().getStudentId(),
			"name", post.getWriter().getName()
		));
		return map;
	}
}
