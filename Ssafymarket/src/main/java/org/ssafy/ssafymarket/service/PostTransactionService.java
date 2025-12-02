package org.ssafy.ssafymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssafy.ssafymarket.entity.Post;
import org.ssafy.ssafymarket.entity.PostImage;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.ChatRoomRepository;
import org.ssafy.ssafymarket.repository.PostLikeRepository;
import org.ssafy.ssafymarket.repository.PostRepository;
import org.ssafy.ssafymarket.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostTransactionService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final PostLikeRepository postLikeRepository;
	private final ChatRoomRepository chatRoomRepository;

	/* ===================== 상태 변경 ===================== */

	@Transactional
	public Map<String, Object> updatePostStatus(Long postId,
		String studentId,
		String status) {

		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		if (!post.getWriter().getStudentId().equals(studentId)) {
			throw new IllegalStateException("본인의 게시글만 상태를 변경할 수 있습니다.");
		}

		Post.PostStatus newStatus = Post.PostStatus.valueOf(status);
		post.setStatus(newStatus);
		postRepository.save(post);

		log.info("판매 상태 변경 - postId: {}, newStatus: {}", postId, newStatus);

		return Map.of(
			"success", true,
			"message", "판매 상태가 변경되었습니다.",
			"status", newStatus
		);
	}

	/* ===================== 판매 완료 처리 ===================== */

	@Transactional
	public Map<String, Object> completePost(Long postId,
		String sellerId,
		String buyerId) {

		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		if (!post.getWriter().getStudentId().equals(sellerId)) {
			throw new IllegalStateException("본인의 게시글만 판매완료 처리할 수 있습니다.");
		}

		User buyer = userRepository.findByStudentId(buyerId)
			.orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다."));

		post.setStatus(Post.PostStatus.판매완료);
		post.setBuyer(buyer);
		postRepository.save(post);

		log.info("판매 완료 처리 - postId: {}, seller: {}, buyer: {}", postId, sellerId, buyerId);

		return Map.of(
			"success", true,
			"message", "판매가 완료되었습니다.",
			"buyerId", buyerId,
			"buyerName", buyer.getName()
		);
	}

	/* ===================== 내가 판매중인 글 ===================== */

	public Map<String, Object> getMySelling(String studentId) {

		User user = userRepository.findByStudentId(studentId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		List<Post> posts = postRepository.findByWriter(user).stream()
			.filter(post -> post.getStatus() == Post.PostStatus.판매중)
			.collect(Collectors.toList());

		List<Map<String, Object>> postList = posts.stream()
			.map(this::convertPostToMap)
			.collect(Collectors.toList());

		log.info("판매중 게시글 조회 - userId: {}, count: {}", studentId, postList.size());

		return Map.of(
			"success", true,
			"posts", postList,
			"count", postList.size()
		);
	}

	/* ===================== 내가 판매완료한 글 ===================== */

	public Map<String, Object> getMySold(String studentId) {

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

		return Map.of(
			"success", true,
			"posts", postList,
			"count", postList.size()
		);
	}

	/* ===================== 내가 구매한 글 ===================== */

	public Map<String, Object> getMyPurchased(String studentId) {

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

		return Map.of(
			"success", true,
			"posts", postList,
			"count", postList.size()
		);
	}

	/* ===================== 거래내역 통합 ===================== */

	public Map<String, Object> getMyTransactions(String studentId) {

		User user = userRepository.findByStudentId(studentId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		// 판매완료
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
				transaction.put("completedAt", post.getCreatedAt()); // TODO: 별도 완료시간 필드 생기면 교체
				return transaction;
			})
			.collect(Collectors.toList());

		// 구매
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

		List<Map<String, Object>> allTransactions = new ArrayList<>();
		allTransactions.addAll(soldList);
		allTransactions.addAll(purchasedList);

		allTransactions.sort((a, b) -> {
			Map<String, Object> postA = (Map<String, Object>) a.get("post");
			Map<String, Object> postB = (Map<String, Object>) b.get("post");
			return ((Comparable) postB.get("createdAt")).compareTo(postA.get("createdAt"));
		});

		log.info("거래내역 조회 - userId: {}, sold: {}, purchased: {}",
			studentId, soldList.size(), purchasedList.size());

		return Map.of(
			"success", true,
			"transactions", allTransactions,
			"soldCount", soldList.size(),
			"purchasedCount", purchasedList.size(),
			"totalCount", allTransactions.size()
		);
	}

	/* ===================== Post → Map 변환 ===================== */

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
