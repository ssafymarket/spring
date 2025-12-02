package org.ssafy.ssafymarket.controller;


import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import org.ssafy.ssafymarket.service.PostService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Validated
@Slf4j
public class PostController {

	private final PostService postService;

	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<Map<String, Object>> createPost(
		@RequestPart("files") @NotNull List<MultipartFile> files,
		@RequestParam("title") @NotNull String title,
		@RequestParam("price") @NotNull Integer price,
		@RequestParam("category") @NotNull String category,
		@RequestParam(value = "description", required = false) String description
	) {
		try {
			Map<String, Object> body = postService.createPost(files, title, price, category, description);
			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("게시글 생성 실패 - 서버 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "서버 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> getAllPosts(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "latest") String sort,
		Authentication authentication
	) {
		try {
			String studentId = (authentication != null) ? authentication.getName() : null;
			Map<String, Object> body = postService.getAllPosts(page, size, sort, studentId);
			return ResponseEntity.ok(body);
		} catch (Exception e) {
			log.error("게시글 목록 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "게시글 목록 조회 실패: " + e.getMessage()));
		}
	}

	@GetMapping("/{postId}")
	public ResponseEntity<Map<String, Object>> getPost(@PathVariable Long postId) {
		try {
			Map<String, Object> body = postService.getPostDetail(postId);
			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "게시글 조회 실패: " + e.getMessage()));
		}
	}

	@PutMapping("/{postId}")
	public ResponseEntity<Map<String, Object>> updatePost(
		@PathVariable Long postId,
		@RequestParam(required = false) String title,
		@RequestParam(required = false) Integer price,
		@RequestParam(required = false) String category,
		@RequestParam(required = false) String description,
		@RequestParam(value = "newImages", required = false) List<MultipartFile> newImages,
		@RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
		Authentication authentication
	) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}
			String studentId = authentication.getName();
			Map<String, Object> body = postService.updatePost(
				postId, studentId, title, price, category, description, newImages, deleteImageIds
			);
			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("게시글 수정 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "게시글 수정 실패: " + e.getMessage()));
		}
	}

	@DeleteMapping("/{postId}")
	public ResponseEntity<Map<String, Object>> deletePost(
		@PathVariable Long postId,
		Authentication authentication
	) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}

			String studentId = authentication.getName();
			boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

			Map<String, Object> body = postService.deletePost(postId, studentId, isAdmin);
			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("게시글 삭제 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "게시글 삭제 실패: " + e.getMessage()));
		}
	}

	@PatchMapping("/{postId}/status")
	public ResponseEntity<Map<String, Object>> updatePostStatus(
		@PathVariable Long postId,
		@RequestParam String status,
		Authentication authentication
	) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}
			String studentId = authentication.getName();
			Map<String, Object> body = postService.updatePostStatus(postId, studentId, status);
			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("상태 변경 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "상태 변경 실패: " + e.getMessage()));
		}
	}

	@PatchMapping("/{postId}/complete")
	public ResponseEntity<Map<String, Object>> completePost(
		@PathVariable Long postId,
		@RequestParam String buyerId,
		Authentication authentication
	) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}
			String writerId = authentication.getName();
			Map<String, Object> body = postService.completePost(postId, writerId, buyerId);
			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("판매완료 처리 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "판매완료 처리 실패: " + e.getMessage()));
		}
	}

	@GetMapping("/category")
	public ResponseEntity<Map<String, Object>> getPostsByCategory(
		@RequestParam String name,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "latest") String sort,
		Authentication authentication
	) {
		try {
			String studentId = (authentication != null) ? authentication.getName() : null;
			Map<String, Object> body = postService.getPostsByCategory(name, page, size, sort, studentId);
			return ResponseEntity.ok(body);
		} catch (Exception e) {
			log.error("카테고리별 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	@GetMapping("/search")
	public ResponseEntity<Map<String, Object>> searchPosts(
		@RequestParam String keyword,
		@RequestParam(required = false) String status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "latest") String sort,
		Authentication authentication
	) {
		try {
			String studentId = (authentication != null) ? authentication.getName() : null;
			Map<String, Object> body = postService.searchPosts(keyword, status, page, size, sort, studentId);
			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("게시글 검색 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "게시글 검색 실패: " + e.getMessage()));
		}
	}

	@PostMapping("/{postId}/like")
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
			Map<String, Object> body = postService.addLike(postId, studentId);
			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("좋아요 추가 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "좋아요 추가 실패: " + e.getMessage()));
		}
	}

	@DeleteMapping("/{postId}/like")
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
			Map<String, Object> body = postService.removeLike(postId, studentId);
			return ResponseEntity.ok(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("좋아요 취소 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "좋아요 취소 실패: " + e.getMessage()));
		}
	}

	@GetMapping("/liked")
	public ResponseEntity<Map<String, Object>> getLikedPosts(Authentication authentication) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}
			String studentId = authentication.getName();
			Map<String, Object> body = postService.getLikedPosts(studentId);
			return ResponseEntity.ok(body);
		} catch (Exception e) {
			log.error("좋아요한 게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

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
			boolean isLiked = postService.isPostLiked(postId, studentId);

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

	// 나머지 "내가 판매중/완료/구매/거래내역" 같은 것도 동일 패턴:
	// - 인증 체크 (없으면 401)
	// - studentId 꺼내기
	// - postService.xxx(studentId) 호출
	// - 결과를 ResponseEntity로 감싸서 리턴
}
