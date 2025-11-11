package org.ssafy.ssafymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.ssafy.ssafymarket.entity.Post;
import org.ssafy.ssafymarket.entity.PostImage;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.ChatRoomRepository;
import org.ssafy.ssafymarket.repository.PostLikeRepository;
import org.ssafy.ssafymarket.repository.PostRepository;
import org.ssafy.ssafymarket.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게시글 거래 관련 컨트롤러
 * - 판매상태 변경
 * - 판매완료 처리
 * - 판매/구매 내역 조회
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
@Slf4j
public class PostTransactionController {

	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final PostLikeRepository postLikeRepository;
	private final ChatRoomRepository chatRoomRepository;

	/**
	 * 판매 상태 변경 (판매중 <-> 판매완료)
	 * PATCH /api/posts/{postId}/status?status=판매중
	 */
	@PatchMapping("/{postId}/status")
	@Transactional
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
			Post post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			if (!post.getWriter().getStudentId().equals(studentId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("success", false, "message", "본인의 게시글만 상태를 변경할 수 있습니다."));
			}

			Post.PostStatus newStatus = Post.PostStatus.valueOf(status);
			post.setStatus(newStatus);
			postRepository.save(post);

			log.info("판매 상태 변경 - postId: {}, status: {} -> {}", postId, post.getStatus(), newStatus);

			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "판매 상태가 변경되었습니다.",
				"status", newStatus
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", "잘못된 상태 값입니다."));
		} catch (Exception e) {
			log.error("상태 변경 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "상태 변경 실패: " + e.getMessage()));
		}
	}

	/**
	 * 판매 완료 처리 (구매자 정보 포함)
	 * PATCH /api/posts/{postId}/complete?buyerId=2024001
	 */
	@PatchMapping("/{postId}/complete")
	@Transactional
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

			String studentId = authentication.getName();
			Post post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			if (!post.getWriter().getStudentId().equals(studentId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("success", false, "message", "본인의 게시글만 판매완료 처리할 수 있습니다."));
			}

			User buyer = userRepository.findByStudentId(buyerId)
				.orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다."));

			post.setStatus(Post.PostStatus.판매완료);
			post.setBuyer(buyer);
			postRepository.save(post);

			log.info("판매 완료 처리 - postId: {}, seller: {}, buyer: {}", postId, studentId, buyerId);

			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "판매가 완료되었습니다.",
				"buyerId", buyerId,
				"buyerName", buyer.getName()
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("판매완료 처리 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "판매완료 처리 실패: " + e.getMessage()));
		}
	}

	/**
	 * 내가 판매중인 게시글 조회
	 * GET /api/posts/my/selling
	 */
	@GetMapping("/my/selling")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getMySelling(Authentication authentication) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}

			String studentId = authentication.getName();
			User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			List<Post> posts = postRepository.findByWriter(user).stream()
				.filter(post -> post.getStatus() == Post.PostStatus.판매중)
				.collect(Collectors.toList());

			List<Map<String, Object>> postList = posts.stream()
				.map(this::convertPostToMap)
				.collect(Collectors.toList());

			log.info("판매중 게시글 조회 - userId: {}, count: {}", studentId, postList.size());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"posts", postList,
				"count", postList.size()
			));
		} catch (Exception e) {
			log.error("판매중 게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 내가 판매완료한 게시글 조회
	 * GET /api/posts/my/sold
	 */
	@GetMapping("/my/sold")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getMySold(Authentication authentication) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}

			String studentId = authentication.getName();
			User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			List<Post> posts = postRepository.findByWriter(user).stream()
				.filter(post -> post.getStatus() == Post.PostStatus.판매완료)
				.collect(Collectors.toList());

			List<Map<String, Object>> postList = posts.stream()
				.map(post -> {
					Map<String, Object> postMap = convertPostToMap(post);
					if (post.getBuyer() != null) {
						postMap.put("buyerName", post.getBuyer().getName());
						postMap.put("buyerId", post.getBuyer().getStudentId());
					}
					return postMap;
				})
				.collect(Collectors.toList());

			log.info("판매완료 게시글 조회 - userId: {}, count: {}", studentId, postList.size());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"posts", postList,
				"count", postList.size()
			));
		} catch (Exception e) {
			log.error("판매완료 게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 내가 구매한 게시글 조회
	 * GET /api/posts/my/purchased
	 */
	@GetMapping("/my/purchased")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getMyPurchased(Authentication authentication) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}

			String studentId = authentication.getName();
			User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			List<Post> posts = postRepository.findByBuyer(user);

			List<Map<String, Object>> postList = posts.stream()
				.map(post -> {
					Map<String, Object> postMap = convertPostToMap(post);
					postMap.put("sellerName", post.getWriter().getName());
					postMap.put("sellerId", post.getWriter().getStudentId());
					return postMap;
				})
				.collect(Collectors.toList());

			log.info("구매 게시글 조회 - userId: {}, count: {}", studentId, postList.size());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"posts", postList,
				"count", postList.size()
			));
		} catch (Exception e) {
			log.error("구매 게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 거래내역 통합 조회 (판매 + 구매)
	 * GET /api/posts/my/transactions
	 */
	@GetMapping("/my/transactions")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getMyTransactions(Authentication authentication) {
		try {
			if (authentication == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("success", false, "message", "인증이 필요합니다."));
			}

			String studentId = authentication.getName();
			User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			// 판매완료한 게시글
			List<Map<String, Object>> soldList = postRepository.findByWriter(user).stream()
				.filter(post -> post.getStatus() == Post.PostStatus.판매완료)
				.map(post -> {
					Map<String, Object> transaction = new HashMap<>();
					transaction.put("type", "판매");
					transaction.put("post", convertPostToMap(post));
					if (post.getBuyer() != null) {
						transaction.put("otherParty", Map.of(
							"studentId", post.getBuyer().getStudentId(),
							"name", post.getBuyer().getName()
						));
					}
					transaction.put("completedAt", post.getCreatedAt());
					return transaction;
				})
				.collect(Collectors.toList());

			// 구매한 게시글
			List<Map<String, Object>> purchasedList = postRepository.findByBuyer(user).stream()
				.map(post -> {
					Map<String, Object> transaction = new HashMap<>();
					transaction.put("type", "구매");
					transaction.put("post", convertPostToMap(post));
					transaction.put("otherParty", Map.of(
						"studentId", post.getWriter().getStudentId(),
						"name", post.getWriter().getName()
					));
					transaction.put("completedAt", post.getCreatedAt());
					return transaction;
				})
				.collect(Collectors.toList());

			// 통합 및 최신순 정렬
			List<Map<String, Object>> allTransactions = new ArrayList<>();
			allTransactions.addAll(soldList);
			allTransactions.addAll(purchasedList);

			// createdAt 기준 최신순 정렬
			allTransactions.sort((a, b) -> {
				Map<String, Object> postA = (Map<String, Object>) a.get("post");
				Map<String, Object> postB = (Map<String, Object>) b.get("post");
				return ((Comparable) postB.get("createdAt")).compareTo(postA.get("createdAt"));
			});

			log.info("거래내역 조회 - userId: {}, sold: {}, purchased: {}",
					studentId, soldList.size(), purchasedList.size());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"transactions", allTransactions,
				"soldCount", soldList.size(),
				"purchasedCount", purchasedList.size(),
				"totalCount", allTransactions.size()
			));
		} catch (Exception e) {
			log.error("거래내역 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * Post를 Map으로 변환하는 헬퍼 메서드
	 */
	private Map<String, Object> convertPostToMap(Post post) {
		Map<String, Object> postMap = new HashMap<>();
		postMap.put("postId", post.getPostId());
		postMap.put("title", post.getTitle());
		postMap.put("price", post.getPrice());
		postMap.put("category", post.getCategory());
		postMap.put("description", post.getDescription());
		postMap.put("status", post.getStatus());
		postMap.put("createdAt", post.getCreatedAt());
		postMap.put("chatRoomCount", chatRoomRepository.countByPost_PostId(post.getPostId()));
		postMap.put("likeCount", postLikeRepository.countByPostId(post.getPostId()));
		postMap.put("writer", post.getWriter().getName());

		List<String> imageUrls = post.getImages().stream()
			.map(PostImage::getImageUrl)
			.collect(Collectors.toList());
		postMap.put("images", imageUrls);

		return postMap;
	}
}
