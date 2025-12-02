package org.ssafy.ssafymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.ssafy.ssafymarket.service.PostLikeService;

import java.util.Map;

/**
 * 게시글 좋아요(관심목록) 관련 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
@Slf4j
public class PostLikeController {

	private final PostLikeService postLikeService;

	/**
	 * 좋아요 추가
	 * POST /api/post/{postId}/like
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
			Map<String, Object> body = postLikeService.addLike(postId, studentId);
			return ResponseEntity.ok(body);

		} catch (IllegalArgumentException e) { // 게시글 없음 등
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (IllegalStateException e) { // 이미 좋아요
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("좋아요 추가 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "좋아요 추가 실패: " + e.getMessage()));
		}
	}

	/**
	 * 좋아요 취소
	 * DELETE /api/post/{postId}/like
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
			Map<String, Object> body = postLikeService.removeLike(postId, studentId);
			return ResponseEntity.ok(body);

		} catch (IllegalArgumentException e) { // 좋아요 안 한 글
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
	 * GET /api/post/liked
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
			Map<String, Object> body = postLikeService.getLikedPosts(studentId);
			return ResponseEntity.ok(body);

		} catch (Exception e) {
			log.error("좋아요한 게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 좋아요 여부 확인
	 * GET /api/post/{postId}/like/check
	 */
	@GetMapping("/{postId}/like/check")
	public ResponseEntity<Map<String, Object>> checkLike(
		@PathVariable Long postId,
		Authentication authentication
	) {
		try {
			if (authentication == null) {
				// 비로그인 사용자는 무조건 false
				return ResponseEntity.ok(Map.of(
					"success", true,
					"isLiked", false
				));
			}

			String studentId = authentication.getName();
			boolean isLiked = postLikeService.isLiked(postId, studentId);

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
}
