package org.ssafy.ssafymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.ssafy.ssafymarket.service.PostTransactionService;

import java.util.Map;

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

	private final PostTransactionService postTransactionService;

	/**
	 * 판매 상태 변경 (판매중 <-> 판매완료)
	 * PATCH /api/post/{postId}/status?status=판매중
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
			Map<String, Object> body = postTransactionService.updatePostStatus(postId, studentId, status);
			return ResponseEntity.ok(body);

		} catch (IllegalArgumentException e) { // 잘못된 상태 값, 게시글 없음 등
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (IllegalStateException e) { // 소유자 아님
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("상태 변경 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "상태 변경 실패: " + e.getMessage()));
		}
	}

	/**
	 * 판매 완료 처리 (구매자 정보 포함)
	 * PATCH /api/post/{postId}/complete?buyerId=2024001
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

			String sellerId = authentication.getName();
			Map<String, Object> body = postTransactionService.completePost(postId, sellerId, buyerId);
			return ResponseEntity.ok(body);

		} catch (IllegalArgumentException e) { // 게시글/구매자 없음
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (IllegalStateException e) { // 본인 글 아님
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			log.error("판매완료 처리 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "판매완료 처리 실패: " + e.getMessage()));
		}
	}

	/**
	 * 내가 판매중인 게시글 조회
	 * GET /api/post/my/selling
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
			Map<String, Object> body = postTransactionService.getMySelling(studentId);
			return ResponseEntity.ok(body);

		} catch (Exception e) {
			log.error("판매중 게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 내가 판매완료한 게시글 조회
	 * GET /api/post/my/sold
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
			Map<String, Object> body = postTransactionService.getMySold(studentId);
			return ResponseEntity.ok(body);

		} catch (Exception e) {
			log.error("판매완료 게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 내가 구매한 게시글 조회
	 * GET /api/post/my/purchased
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
			Map<String, Object> body = postTransactionService.getMyPurchased(studentId);
			return ResponseEntity.ok(body);

		} catch (Exception e) {
			log.error("구매 게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 거래내역 통합 조회 (판매 + 구매)
	 * GET /api/post/my/transactions
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
			Map<String, Object> body = postTransactionService.getMyTransactions(studentId);
			return ResponseEntity.ok(body);

		} catch (Exception e) {
			log.error("거래내역 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}
}
