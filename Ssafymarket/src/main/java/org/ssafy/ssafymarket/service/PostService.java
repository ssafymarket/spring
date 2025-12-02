package org.ssafy.ssafymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.ssafy.ssafymarket.dto.PostCreateRequest;
import org.ssafy.ssafymarket.entity.Post;
import org.ssafy.ssafymarket.entity.PostImage;
import org.ssafy.ssafymarket.entity.PostLike;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.minio.MinioService;
import org.ssafy.ssafymarket.repository.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostService {

	private final MinioService minioService;
	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;
	private final UserRepository userRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final PostLikeRepository postLikeRepository;

	/* ===================== 게시글 생성 ===================== */

	@Transactional
	public Map<String, Object> createPost(List<MultipartFile> files,
		String title,
		Integer price,
		String category,
		String description) throws Exception {

		if (files == null || files.isEmpty()) {
			throw new IllegalArgumentException("최소 1개의 이미지가 필요합니다.");
		}

		if (files.size() > 10) {
			throw new IllegalArgumentException("이미지는 최대 10개까지 업로드 가능합니다.");
		}

		PostCreateRequest newPost = PostCreateRequest.builder()
			.title(title)
			.price(price)
			.category(category)
			.description(description)
			.build();

		Long postId = minioService.saveImages(files, newPost);
		log.info("게시글 생성 성공 - postId: {}", postId);

		return Map.of(
			"success", true,
			"postId", postId,
			"message", "게시글이 성공적으로 생성되었습니다."
		);
	}

	/* ===================== 전체 게시글 조회 ===================== */

	public Map<String, Object> getAllPosts(int page,
		int size,
		String sort,
		String studentIdOrNull) {

		Page<Post> postPage;
		Map<String, Object> response = new HashMap<>();

		// 비로그인
		if (studentIdOrNull == null) {
			if ("popular".equalsIgnoreCase(sort)) {
				Pageable pageable = PageRequest.of(page, size);
				postPage = postRepository.findAllByPopularity(pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				postPage = postRepository.findAll(pageable);
			}
		} else {
			// 로그인: 본인 캠퍼스만
			User user = userRepository.findByStudentId(studentIdOrNull)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			if ("popular".equalsIgnoreCase(sort)) {
				Pageable pageable = PageRequest.of(page, size);
				postPage = postRepository.findByCampusByPopularity(user.getCampus(), pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				postPage = postRepository.findByCampus(user.getCampus(), pageable);
			}
			response.put("campus", user.getCampus().toString());
		}

		List<Map<String, Object>> postList = postPage.getContent().stream()
			.map(this::convertPostToMap)
			.collect(Collectors.toList());

		response.put("success", true);
		response.put("posts", postList);
		response.put("currentPage", postPage.getNumber());
		response.put("totalPages", postPage.getTotalPages());
		response.put("totalItems", postPage.getTotalElements());
		response.put("pageSize", postPage.getSize());

		return response;
	}

	/* ===================== 게시글 상세 조회 ===================== */

	public Map<String, Object> getPostDetail(Long postId) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		Map<String, Object> postMap = new HashMap<>();
		postMap.put("postId", post.getPostId());
		postMap.put("title", post.getTitle());
		postMap.put("price", post.getPrice());
		postMap.put("category", post.getCategory());
		postMap.put("description", post.getDescription());
		postMap.put("status", post.getStatus());
		postMap.put("createdAt", post.getCreatedAt());
		postMap.put("chatRoomCount", chatRoomRepository.countByPost_PostId(postId));
		postMap.put("likeCount", postLikeRepository.countByPostId(postId));
		postMap.put("writer", Map.of(
			"studentId", post.getWriter().getStudentId(),
			"name", post.getWriter().getName()
		));

		List<Map<String, ? extends Serializable>> images = post.getImages().stream()
			.map(img -> Map.of(
				"imageId", img.getImageId(),
				"imageUrl", img.getImageUrl(),
				"imageOrder", img.getImageOrder()
			))
			.collect(Collectors.toList());
		postMap.put("images", images);

		return Map.of(
			"success", true,
			"post", postMap
		);
	}

	/* ===================== 게시글 수정 ===================== */

	@Transactional
	public Map<String, Object> updatePost(Long postId,
		String studentId,
		String title,
		Integer price,
		String category,
		String description,
		List<MultipartFile> newImages,
		List<Long> deleteImageIds) throws Exception {

		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		if (!post.getWriter().getStudentId().equals(studentId)) {
			throw new IllegalStateException("본인의 게시글만 수정할 수 있습니다.");
		}

		// 텍스트 정보 수정
		if (title != null) post.setTitle(title);
		if (price != null) post.setPrice(price);
		if (category != null) post.setCategory(category);
		if (description != null) post.setDescription(description);

		int currentImageCount = post.getImages().size();
		int deleteCount = (deleteImageIds != null) ? deleteImageIds.size() : 0;
		int addCount = (newImages != null) ? newImages.size() : 0;
		int finalImageCount = currentImageCount - deleteCount + addCount;

		if (finalImageCount < 1) {
			throw new IllegalArgumentException("최소 1개의 이미지는 유지해야 합니다.");
		}
		if (finalImageCount > 10) {
			throw new IllegalArgumentException("이미지는 최대 10개까지 가능합니다.");
		}

		// 이미지 삭제
		if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
			List<PostImage> imagesToDelete = post.getImages().stream()
				.filter(img -> deleteImageIds.contains(img.getImageId()))
				.toList();

			for (PostImage image : imagesToDelete) {
				try {
					minioService.deleteFile(image.getImageUrl());
				} catch (Exception e) {
					log.warn("MinIO 이미지 삭제 실패 (계속 진행): {}", e.getMessage());
				}
				post.getImages().remove(image);
			}
			log.info("이미지 삭제 완료 - postId: {}, 삭제 개수: {}", postId, imagesToDelete.size());
		}

		// 새 이미지 추가
		if (newImages != null && !newImages.isEmpty()) {
			for (MultipartFile file : newImages) {
				if (!file.isEmpty()) {
					String imageUrl = minioService.uploadFile(file);
					post.addImage(imageUrl);
				}
			}
			log.info("이미지 추가 완료 - postId: {}, 추가 개수: {}", postId, newImages.size());
		}

		postRepository.save(post);

		return Map.of(
			"success", true,
			"message", "게시글이 수정되었습니다.",
			"postId", postId,
			"totalImages", post.getImages().size()
		);
	}

	/* ===================== 게시글 삭제 ===================== */

	@Transactional
	public Map<String, Object> deletePost(Long postId,
		String studentId,
		boolean isAdmin) {

		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		boolean isOwner = post.getWriter().getStudentId().equals(studentId);

		if (!isOwner && !isAdmin) {
			throw new IllegalStateException("본인의 게시글이거나 관리자만 삭제할 수 있습니다.");
		}

		List<PostLike> likes = postLikeRepository.findByPostId(postId);
		if (!likes.isEmpty()) {
			postLikeRepository.deleteAll(likes);
			log.info("게시글 삭제 전 좋아요 삭제: postId={}, 좋아요 수={}", postId, likes.size());
		}

		postRepository.delete(post);

		String deletedBy = isAdmin && !isOwner ? "관리자" : "작성자";
		log.info("게시글 삭제: postId={}, deletedBy={} ({})", postId, deletedBy, studentId);

		return Map.of(
			"success", true,
			"message", "게시글이 삭제되었습니다.",
			"deletedBy", deletedBy
		);
	}

	/* ===================== 상태 변경/완료 ===================== */

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

		return Map.of(
			"success", true,
			"message", "판매 상태가 변경되었습니다.",
			"status", newStatus
		);
	}

	@Transactional
	public Map<String, Object> completePost(Long postId,
		String writerId,
		String buyerId) {

		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		if (!post.getWriter().getStudentId().equals(writerId)) {
			throw new IllegalStateException("본인의 게시글만 판매완료 처리할 수 있습니다.");
		}

		User buyer = userRepository.findByStudentId(buyerId)
			.orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다."));

		post.setStatus(Post.PostStatus.판매완료);
		post.setBuyer(buyer);
		postRepository.save(post);

		return Map.of(
			"success", true,
			"message", "판매가 완료되었습니다.",
			"buyerId", buyerId,
			"buyerName", buyer.getName()
		);
	}

	/* ===================== 카테고리 / 검색 ===================== */

	public Map<String, Object> getPostsByCategory(String category,
		int page,
		int size,
		String sort,
		String studentIdOrNull) {

		Page<Post> postPage;
		Map<String, Object> response = new HashMap<>();

		if (studentIdOrNull == null) {
			if ("popular".equalsIgnoreCase(sort)) {
				Pageable pageable = PageRequest.of(page, size);
				postPage = postRepository.findByCategoryByPopularity(category, pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				postPage = postRepository.findByCategory(category, pageable);
			}
		} else {
			User user = userRepository.findByStudentId(studentIdOrNull)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			if ("popular".equalsIgnoreCase(sort)) {
				Pageable pageable = PageRequest.of(page, size);
				postPage = postRepository.findByCampusAndCategoryByPopularity(
					user.getCampus(), category, pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				postPage = postRepository.findByCampusAndCategory(
					user.getCampus(), category, pageable);
			}

			response.put("campus", user.getCampus().toString());
		}

		List<Map<String, Object>> postList = postPage.getContent().stream()
			.map(this::convertPostToMap)
			.collect(Collectors.toList());

		response.put("success", true);
		response.put("category", category);
		response.put("posts", postList);
		response.put("currentPage", postPage.getNumber());
		response.put("totalPages", postPage.getTotalPages());
		response.put("totalItems", postPage.getTotalElements());
		response.put("pageSize", postPage.getSize());

		return response;
	}

	public Map<String, Object> searchPosts(String keyword,
		String status,
		int page,
		int size,
		String sort,
		String studentIdOrNull) {

		if (keyword == null || keyword.trim().isEmpty()) {
			throw new IllegalArgumentException("검색 키워드를 입력해주세요.");
		}

		keyword = keyword.trim();
		Page<Post> postPage;
		Map<String, Object> response = new HashMap<>();
		boolean isPopular = "popular".equalsIgnoreCase(sort);

		// 비로그인
		if (studentIdOrNull == null) {
			postPage = searchForAnonymous(keyword, status, page, size, sort, isPopular);
		} else {
			User user = userRepository.findByStudentId(studentIdOrNull)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			postPage = searchForCampus(user, keyword, status, page, size, sort, isPopular);
			response.put("campus", user.getCampus().toString());
		}

		List<Map<String, Object>> postList = postPage.getContent().stream()
			.map(this::convertPostToMap)
			.collect(Collectors.toList());

		response.put("success", true);
		response.put("posts", postList);
		response.put("currentPage", postPage.getNumber());
		response.put("totalPages", postPage.getTotalPages());
		response.put("totalItems", postPage.getTotalElements());
		response.put("pageSize", postPage.getSize());
		response.put("keyword", keyword);

		return response;
	}

	private Page<Post> searchForAnonymous(String keyword,
		String status,
		int page,
		int size,
		String sort,
		boolean isPopular) {

		if (status != null && !status.trim().isEmpty()) {
			Post.PostStatus postStatus;
			try {
				postStatus = Post.PostStatus.valueOf(status);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("잘못된 판매상태입니다. (판매중, 판매완료 중 선택)");
			}

			if (isPopular) {
				Pageable pageable = PageRequest.of(page, size);
				return postRepository.searchByKeywordAndStatusByPopularity(keyword, postStatus, pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				return postRepository.searchByKeywordAndStatus(keyword, postStatus, pageable);
			}
		} else {
			if (isPopular) {
				Pageable pageable = PageRequest.of(page, size);
				return postRepository.searchByKeywordByPopularity(keyword, pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				return postRepository.searchByKeyword(keyword, pageable);
			}
		}
	}

	private Page<Post> searchForCampus(User user,
		String keyword,
		String status,
		int page,
		int size,
		String sort,
		boolean isPopular) {

		if (status != null && !status.trim().isEmpty()) {
			Post.PostStatus postStatus;
			try {
				postStatus = Post.PostStatus.valueOf(status);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("잘못된 판매상태입니다. (판매중, 판매완료 중 선택)");
			}

			if (isPopular) {
				Pageable pageable = PageRequest.of(page, size);
				return postRepository.searchByCampusAndKeywordAndStatusByPopularity(
					user.getCampus(), keyword, postStatus, pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				return postRepository.searchByCampusAndKeywordAndStatus(
					user.getCampus(), keyword, postStatus, pageable);
			}
		} else {
			if (isPopular) {
				Pageable pageable = PageRequest.of(page, size);
				return postRepository.searchByCampusAndKeywordByPopularity(
					user.getCampus(), keyword, pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				return postRepository.searchByCampusAndKeyword(
					user.getCampus(), keyword, pageable);
			}
		}
	}

	/* ===================== 좋아요 관련 ===================== */

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

		return Map.of(
			"success", true,
			"message", "좋아요를 추가했습니다.",
			"likeCount", likeCount
		);
	}

	@Transactional
	public Map<String, Object> removeLike(Long postId, String studentId) {

		PostLike postLike = postLikeRepository.findByUserIdAndPostId(studentId, postId)
			.orElseThrow(() -> new IllegalArgumentException("좋아요하지 않은 게시글입니다."));

		postLikeRepository.delete(postLike);
		long likeCount = postLikeRepository.countByPostId(postId);

		return Map.of(
			"success", true,
			"message", "좋아요를 취소했습니다.",
			"likeCount", likeCount
		);
	}

	public Map<String, Object> getLikedPosts(String studentId) {
		List<PostLike> likes = postLikeRepository.findByUserId(studentId);

		List<Map<String, Object>> postList = likes.stream()
			.map(like -> {
				Post post = like.getPost();
				Map<String, Object> postMap = convertPostToMap(post);
				postMap.put("likedAt", like.getLikedAt());
				return postMap;
			})
			.collect(Collectors.toList());

		return Map.of(
			"success", true,
			"posts", postList,
			"count", postList.size()
		);
	}

	public boolean isPostLiked(Long postId, String studentId) {
		return postLikeRepository.existsByUserIdAndPostId(studentId, postId);
	}

	/* ===================== 공통 헬퍼 ===================== */

	private Pageable createPageable(int page, int size, String sort) {
		Sort sorting;
		switch (sort.toLowerCase()) {
			case "popular":
				sorting = Sort.by(Sort.Direction.DESC, "likeCount", "createdAt");
				break;
			case "lowprice":
				sorting = Sort.by(Sort.Direction.ASC, "price", "createdAt");
				break;
			case "highprice":
				sorting = Sort.by(Sort.Direction.DESC, "price", "createdAt");
				break;
			case "latest":
			default:
				sorting = Sort.by(Sort.Direction.DESC, "createdAt");
				break;
		}
		return PageRequest.of(page, size, sorting);
	}

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
