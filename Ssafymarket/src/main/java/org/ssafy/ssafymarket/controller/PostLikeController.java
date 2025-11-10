package org.ssafy.ssafymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.ssafy.ssafymarket.entity.Post;
import org.ssafy.ssafymarket.entity.PostLike;
import org.ssafy.ssafymarket.repository.PostLikeRepository;
import org.ssafy.ssafymarket.repository.PostRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게시글 좋아요(관심목록) 관련 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Slf4j
public class PostLikeController {

	private final PostRepository postRepository;
	private final PostLikeRepository postLikeRepository;

	/**
	 * 좋아요 추가
	 * POST /api/posts/{postId}/like
	 */
	@PostMapping("/{postId}/like")
	@Transactional
	public ResponseEntity<Map<String, Object>> addLike(
		@PathVariable Long postId,
		Authentication authentication
	) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}

			String studentId = authentication.getName();

			if (!postRepository.existsById(postId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("success", false, "message", "게시글을 찾을 수 없습니다."));
			}

			if (postLikeRepository.existsByUserIdAndPostId(studentId, postId)) {
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "이미 좋아요한 게시글입니다."));
			}

			PostLike postLike = PostLike.builder()
				.userId(studentId)
				.postId(postId)
				.build();

			postLikeRepository.save(postLike);

			long likeCount = postLikeRepository.countByPostId(postId);

			log.info("좋아요 추가 - postId: {}, userId: {}, likeCount: {}", postId, studentId, likeCount);

			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "좋아요를 추가했습니다.",
				"likeCount", likeCount
			));
		} catch (Exception e) {
			log.error("좋아요 추가 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "좋아요 추가 실패: " + e.getMessage()));
		}
	}

	/**
	 * 좋아요 취소
	 * DELETE /api/posts/{postId}/like
	 */
	@DeleteMapping("/{postId}/like")
	@Transactional
	public ResponseEntity<Map<String, Object>> removeLike(
		@PathVariable Long postId,
		Authentication authentication
	) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}

			String studentId = authentication.getName();

			PostLike postLike = postLikeRepository.findByUserIdAndPostId(studentId, postId)
				.orElseThrow(() -> new IllegalArgumentException("좋아요하지 않은 게시글입니다."));

			postLikeRepository.delete(postLike);

			long likeCount = postLikeRepository.countByPostId(postId);

			log.info("좋아요 취소 - postId: {}, userId: {}, likeCount: {}", postId, studentId, likeCount);

			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "좋아요를 취소했습니다.",
				"likeCount", likeCount
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("좋아요 취소 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "좋아요 취소 실패: " + e.getMessage()));
		}
	}

	/**
	 * 내가 좋아요한 게시글 목록 조회 (관심목록)
	 * GET /api/posts/liked
	 */
	@GetMapping("/liked")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getLikedPosts(Authentication authentication) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}

			String studentId = authentication.getName();

			List<PostLike> likes = postLikeRepository.findByUserId(studentId);

			List<Map<String, Object>> postList = likes.stream()
				.map(like -> {
					Post post = like.getPost();
					return convertPostToMap(post, like);
				})
				.collect(Collectors.toList());

			log.info("관심목록 조회 - userId: {}, count: {}", studentId, postList.size());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"posts", postList,
				"count", postList.size()
			));
		} catch (Exception e) {
			log.error("좋아요한 게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 좋아요 여부 확인
	 * GET /api/posts/{postId}/like/check
	 */
	@GetMapping("/{postId}/like/check")
	public ResponseEntity<Map<String, Object>> checkLike(
		@PathVariable Long postId,
		Authentication authentication
	) {
		try {
			if (authentication == null) {
				return ResponseEntity.ok(Map.of(
					"success", true,
					"isLiked", false
				));
			}

			String studentId = authentication.getName();
			boolean isLiked = postLikeRepository.existsByUserIdAndPostId(studentId, postId);

			return ResponseEntity.ok(Map.of(
				"success", true,
				"isLiked", isLiked
			));
		} catch (Exception e) {
			log.error("좋아요 확인 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "확인 실패: " + e.getMessage()));
		}
	}

	/**
	 * Post 엔티티를 Map으로 변환 (좋아요 정보 포함)
	 */
	private Map<String, Object> convertPostToMap(Post post, PostLike like) {
		return Map.of(
			"postId", post.getPostId(),
			"title", post.getTitle(),
			"price", post.getPrice(),
			"category", post.getCategory() != null ? post.getCategory() : "",
			"description", post.getDescription() != null ? post.getDescription() : "",
			"status", post.getStatus(),
			"createdAt", post.getCreatedAt(),
			"likedAt", like.getLikedAt(),
			"thumbnailUrl", post.getThumbnailUrl() != null ? post.getThumbnailUrl() : "",
			"imageUrls", post.getImageUrls(),
			"likeCount", postLikeRepository.countByPostId(post.getPostId()),
			"chatRoomCount", post.getChatRoomCount(),
			"writer", Map.of(
				"studentId", post.getWriter().getStudentId(),
				"name", post.getWriter().getName()
			)
		);
	}
}
