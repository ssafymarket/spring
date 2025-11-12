package org.ssafy.ssafymarket.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
import org.ssafy.ssafymarket.dto.PostCreateRequest;
import org.ssafy.ssafymarket.entity.Post;
import org.ssafy.ssafymarket.entity.PostImage;
import org.ssafy.ssafymarket.entity.PostLike;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.minio.MinioService;
import org.ssafy.ssafymarket.repository.ChatRoomRepository;
import org.ssafy.ssafymarket.repository.PostImageRepository;
import org.ssafy.ssafymarket.repository.PostLikeRepository;
import org.ssafy.ssafymarket.repository.PostRepository;
import org.ssafy.ssafymarket.repository.UserRepository;

import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Tag(name = "게시글", description = "게시글 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Validated
@Slf4j
public class PostController {
	private final MinioService minioService;
	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;
	private final UserRepository userRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final PostLikeRepository postLikeRepository;

	/**
	 * 게시글 생성 (다중 이미지 지원)
	 *
	 * @param files 이미지 파일 리스트 (최소 1개, 최대 10개)
	 * @param title 게시글 제목
	 * @param price 판매 가격
	 * @param category 카테고리
	 * @param description 상품 설명
	 * @return 생성된 게시글 ID
	 *
	 * 사용 예시:
	 * - 단일 이미지: files[0] = file
	 * - 다중 이미지: files[0] = file1, files[1] = file2, ...
	 */

	@Operation(
		summary = "게시글 생성",
		description = "\t * @param files 이미지 파일 리스트 (최소 1개, 최대 10개)\n"
			+ "\t * @param title 게시글 제목\n"
			+ "\t * @param price 판매 가격\n"
			+ "\t * @param category 카테고리\n"
			+ "\t * @param description 상품 설명\n"
			+ "\t * @return 생성된 게시글 ID"
	)
	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<Map<String, Object>> createPost(
		@RequestPart("files") @NotNull List<MultipartFile> files,
		@RequestParam("title") @NotNull String title,
		@RequestParam("price") @NotNull Integer price,
		@RequestParam("category") @NotNull String category,
		@RequestParam(value = "description", required = false) String description
	) {
		log.info("게시글 생성 요청 - 제목: {}, 가격: {}, 이미지 개수: {}", title, price, files.size());

		try {
			// 입력 검증
			if (files.isEmpty()) {
				return ResponseEntity.badRequest()
					.body(Map.of(
						"success", false,
						"message", "최소 1개의 이미지가 필요합니다."
					));
			}

			if (files.size() > 10) {
				return ResponseEntity.badRequest()
					.body(Map.of(
						"success", false,
						"message", "이미지는 최대 10개까지 업로드 가능합니다."
					));
			}

			// 게시글 생성 요청 DTO
			PostCreateRequest newPost = PostCreateRequest.builder()
				.title(title)
				.price(price)
				.category(category)
				.description(description)
				.build();

			// MinIO 업로드 및 DB 저장
			Long postId = minioService.saveImages(files, newPost);

			log.info("게시글 생성 성공 - postId: {}", postId);

			return ResponseEntity.ok(Map.of(
				"success", true,
				"postId", postId,
				"message", "게시글이 성공적으로 생성되었습니다."
			));

		} catch (IllegalArgumentException e) {
			log.warn("게시글 생성 실패 - 잘못된 요청: {}", e.getMessage());
			return ResponseEntity.badRequest()
				.body(Map.of(
					"success", false,
					"message", e.getMessage()
				));

		} catch (Exception e) {
			log.error("게시글 생성 실패 - 서버 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of(
					"success", false,
					"message", "서버 오류가 발생했습니다: " + e.getMessage()
				));
		}
	}

	/**
	 * 전체 게시글 목록 조회 (페이징 + 정렬)
	 * @param page 페이지 번호 (0부터 시작)
	 * @param size 페이지 크기 (기본 20)
	 * @param sort 정렬 방식 (latest, popular, lowPrice, highPrice)
	 */

	@Operation(
		summary = "전체 게시글 목록 조회",
		description = "\t * @param page 페이지 번호 (0부터 시작)\n"
			+ "\t * @param size 페이지 크기 (기본 20)\n"
			+ "\t * @param sort 정렬 방식 (latest, popular, lowPrice, highPrice)"
	)
	@GetMapping
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getAllPosts(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "latest") String sort
	) {
		try {
			Page<Post> postPage;

			// 인기순은 별도 쿼리 사용
			if ("popular".equalsIgnoreCase(sort)) {
				Pageable pageable = PageRequest.of(page, size);
				postPage = postRepository.findAllByPopularity(pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				postPage = postRepository.findAll(pageable);
			}

			List<Map<String, Object>> postList = postPage.getContent().stream()
				.map(this::convertPostToMap)
				.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"posts", postList,
				"currentPage", postPage.getNumber(),
				"totalPages", postPage.getTotalPages(),
				"totalItems", postPage.getTotalElements(),
				"pageSize", postPage.getSize()
			));
		} catch (Exception e) {
			log.error("게시글 목록 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of(
					"success", false,
					"message", "게시글 목록 조회 실패: " + e.getMessage()
				));
		}
	}

	/**
	 * 특정 게시글 상세 조회
	 */
	@Operation(
		summary = "특정 게시글 상세 조회",
		description = "postid로 상세조회"
	)
	@GetMapping("/{postId}")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getPost(@PathVariable Long postId) {
		try {
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

			List<Map<String, Object>> images = post.getImages().stream()
				.map(img -> {
					Map<String, Object> imgMap = new HashMap<>();
					imgMap.put("imageId", img.getImageId());
					imgMap.put("imageUrl", img.getImageUrl());
					imgMap.put("imageOrder", img.getImageOrder());
					return imgMap;
				})
				.collect(Collectors.toList());
			postMap.put("images", images);

			return ResponseEntity.ok(Map.of(
				"success", true,
				"post", postMap
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of(
					"success", false,
					"message", e.getMessage()
				));
		} catch (Exception e) {
			log.error("게시글 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of(
					"success", false,
					"message", "게시글 조회 실패: " + e.getMessage()
				));
		}
	}

	/**
	 * 게시글 수정 (제목, 가격, 카테고리, 설명, 이미지 수정 가능)
	 */
	@Operation(
		summary = "게시글 수정",
		description = "게시글 수정 (제목, 가격, 카테고리, 설명, 이미지 수정 가능)\n" +
			"- 텍스트 정보: title, price, category, description (선택)\n" +
			"- 이미지: newImages로 새 이미지 추가, deleteImageIds로 삭제할 이미지 ID 목록\n" +
			"- 최소 1개의 이미지는 유지되어야 함"
	)
	@PutMapping("/{postId}")
	@Transactional
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
			Post post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			if (!post.getWriter().getStudentId().equals(studentId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("success", false, "message", "본인의 게시글만 수정할 수 있습니다."));
			}

			// 텍스트 정보 수정
			if (title != null) post.setTitle(title);
			if (price != null) post.setPrice(price);
			if (category != null) post.setCategory(category);
			if (description != null) post.setDescription(description);

			// 이미지 삭제 처리
			int currentImageCount = post.getImages().size();
			int deleteCount = (deleteImageIds != null) ? deleteImageIds.size() : 0;
			int addCount = (newImages != null) ? newImages.size() : 0;
			int finalImageCount = currentImageCount - deleteCount + addCount;

			// 최소 1개 이미지 유지 검증
			if (finalImageCount < 1) {
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "최소 1개의 이미지는 유지해야 합니다."));
			}

			// 최대 10개 검증
			if (finalImageCount > 10) {
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "이미지는 최대 10개까지 가능합니다."));
			}

			// 이미지 삭제
			if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
				List<PostImage> imagesToDelete = post.getImages().stream()
					.filter(img -> deleteImageIds.contains(img.getImageId()))
					.toList();

				for (PostImage image : imagesToDelete) {
					// MinIO에서 삭제 시도 (실패해도 계속 진행)
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

			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "게시글이 수정되었습니다.",
				"postId", postId,
				"totalImages", post.getImages().size()
			));
		} catch (Exception e) {
			log.error("게시글 수정 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "게시글 수정 실패: " + e.getMessage()));
		}
	}

	/**
	 * 게시글 삭제 (본인 또는 관리자만 가능)
	 */
	@Operation(
		summary = "게시글 삭제",
		description = "postId로 삭제"
	)
	@DeleteMapping("/{postId}")
	@Transactional
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
			Post post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

			boolean isOwner = post.getWriter().getStudentId().equals(studentId);

			if (!isOwner && !isAdmin) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("success", false, "message", "본인의 게시글이거나 관리자만 삭제할 수 있습니다."));
			}

			postRepository.delete(post);

			String deletedBy = isAdmin && !isOwner ? "관리자" : "작성자";
			log.info("게시글 삭제: postId={}, deletedBy={} ({})", postId, deletedBy, studentId);

			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "게시글이 삭제되었습니다.",
				"deletedBy", deletedBy
			));
		} catch (Exception e) {
			log.error("게시글 삭제 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "게시글 삭제 실패: " + e.getMessage()));
		}
	}

	/**
	 * 판매 상태 변경 (판매중으로 변경)
	 */
	@Operation(
		summary = "판매상태 변경 ",
		description = "postId로 판매중으로 변경"
	)
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
	 */

	@Operation(
		summary = "판매 완료 처리",
		description = "postId로 처리"
	)
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
	 * 카테고리별 게시글 조회 (페이징 + 정렬)
	 */

	@Operation(
		summary = "카테고리별 게시글 조회",
		description = "카테고리종류, page,size,sort\n"+
			"sort 에들어갈수 있는값 popular,lowprice,highprice,latest"
	)
	@GetMapping("/category")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getPostsByCategory(
		@RequestParam String name,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "latest") String sort
	) {
		String category = name;
		try {
			Page<Post> postPage;

			// 인기순은 별도 쿼리 사용
			if ("popular".equalsIgnoreCase(sort)) {
				Pageable pageable = PageRequest.of(page, size);
				postPage = postRepository.findByCategoryByPopularity(category, pageable);
			} else {
				Pageable pageable = createPageable(page, size, sort);
				postPage = postRepository.findByCategory(category, pageable);
			}

			List<Map<String, Object>> postList = postPage.getContent().stream()
				.map(this::convertPostToMap)
				.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"category", category,
				"posts", postList,
				"currentPage", postPage.getNumber(),
				"totalPages", postPage.getTotalPages(),
				"totalItems", postPage.getTotalElements(),
				"pageSize", postPage.getSize()
			));
		} catch (Exception e) {
			log.error("카테고리별 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 판매 상태별 게시글 조회 (페이징 + 정렬)
	 */
	@Operation(
		summary = "판매 상태별 조회",
		description = "status, pasg,size,sort\n"+
			"sort 에들어갈수 있는값 popular,lowprice,highprice,latest\n"+
			"status 에는 판매중, 판매완료 가 들어갈수 있다."
	)
	@GetMapping("/status/{status}")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getPostsByStatus(
		@PathVariable String status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "latest") String sort
	) {
		try {
			Post.PostStatus postStatus = Post.PostStatus.valueOf(status);
			Pageable pageable = createPageable(page, size, sort);
			Page<Post> postPage = postRepository.findByStatus(postStatus, pageable);

			List<Map<String, Object>> postList = postPage.getContent().stream()
				.map(this::convertPostToMap)
				.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"status", status,
				"posts", postList,
				"currentPage", postPage.getNumber(),
				"totalPages", postPage.getTotalPages(),
				"totalItems", postPage.getTotalElements(),
				"pageSize", postPage.getSize()
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("success", false, "message", "잘못된 상태 값입니다."));
		} catch (Exception e) {
			log.error("상태별 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 사용자별 게시글 조회 (내가 작성한 게시글)
	 */
	@Operation(
		summary = "사용자별 작성한 게시글 조회",
		description = "학번으로 그 학번 유저가 작성한 게시글 조회"
	)
	@GetMapping("/user/{studentId}")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> getPostsByUser(@PathVariable String studentId) {
		try {
			User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			List<Post> posts = postRepository.findByWriter(user);

			List<Map<String, Object>> postList = posts.stream()
				.map(this::convertPostToMap)
				.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"studentId", studentId,
				"posts", postList,
				"count", postList.size()
			));
		} catch (Exception e) {
			log.error("사용자별 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 내가 판매중인 게시글 조회
	 */
	@Operation(
		summary = "내가 판매중인 게시글 조회",
		description = ""
	)
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
	 */
	@Operation(
		summary = "내가 판매완료한 게시글 조회",
		description = ""
	)
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
	 */
	@Operation(
		summary = "내가 구매한 게시글 조회",
		description = ""
	)
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
	 */

	@Operation(
		summary = "거래내역 통합 조회",
		description = "판매내역과 구매 내역 동시에 조회"
	)
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
					transaction.put("completedAt", post.getCreatedAt()); // 실제로는 완료시간 필드가 필요
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
	 * 정렬 방식에 따라 Pageable 생성
	 */
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

	/**
	 * 특정 게시글의 채팅방 개수 조회
	 */
	@Operation(
		summary = "특정 게시글 채팅방 개수 조회",
		description = "postID로 조회"
	)
	@GetMapping("/{postId}/chatrooms/count")
	public ResponseEntity<Map<String, Object>> getChatRoomCount(@PathVariable Long postId) {
		try {
			if (!postRepository.existsById(postId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("success", false, "message", "게시글을 찾을 수 없습니다."));
			}

			long count = chatRoomRepository.countByPost_PostId(postId);

			return ResponseEntity.ok(Map.of(
				"success", true,
				"postId", postId,
				"chatRoomCount", count
			));
		} catch (Exception e) {
			log.error("채팅방 개수 조회 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
		}
	}

	/**
	 * 좋아요 추가
	 */
	@Operation(
		summary = "좋아요 추가",
		description = "postid로 그 게시글에 좋아요를 추가한다."
	)
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
	 */
	@Operation(
		summary = "좋아요 취소",
		description = "postid로 좋아요를 취소한다."
	)
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
	 * 내가 좋아요한 게시글 목록 조회
	 */
	@Operation(
		summary = "내가 좋아요한 게시글 목록 조회",
		description = ""
	)
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
					Map<String, Object> postMap = convertPostToMap(post);
					postMap.put("likedAt", like.getLikedAt());
					return postMap;
				})
				.collect(Collectors.toList());

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
	 */
	@Operation(
		summary = "좋아요 여부 확인",
		description = "게시글의 좋아요갯수 확인 postid 필요"
	)
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
	 * 단일 이미지 업로드 (하위 호환성)
	 * @deprecated createPost()로 통합됨
	 */
	@Deprecated
	@PostMapping(value = "/single", consumes = "multipart/form-data")
	public ResponseEntity<Boolean> uploadAndCreatePostLegacy(
		@RequestPart("file") @NotNull MultipartFile file,
		@RequestParam("title") @NotNull String title,
		@RequestParam("price") @NotNull Integer price,
		@RequestParam("category") @NotNull String category
	) {
		PostCreateRequest newPost = PostCreateRequest.builder()
			.title(title)
			.price(price)
			.category(category)
			.build();
		try {
			log.info("이미지 저장 TRY (단일 이미지 - deprecated)");
			Long postId = minioService.saveImage(file, newPost);
			return ResponseEntity.ok(true);
		} catch (Exception exception) {
			log.error("이미지 저장 실패", exception);
			return ResponseEntity.ok(false);
		}
	}

	/**
	 * 게시글 검색 (제목 + 설명)
	 * GET /api/posts/search?keyword=노트북&status=판매중&page=0&size=20&sort=latest
	 *
	 * @param keyword 검색 키워드 (제목 또는 설명에 포함)
	 * @param status 판매상태 (선택, 없으면 전체 검색)
	 * @param page 페이지 번호 (기본 0)
	 * @param size 페이지 크기 (기본 20)
	 * @param sort 정렬 방식 (latest, popular, lowPrice, highPrice)
	 */
	@Operation(
		summary = "게시글 검색",
		description = "\t * @param keyword 검색 키워드 (제목 또는 설명에 포함)\n"
			+ "\t * @param status 판매상태 (선택, 없으면 전체 검색)\n"
			+ "\t * @param page 페이지 번호 (기본 0)\n"
			+ "\t * @param size 페이지 크기 (기본 20)\n"
			+ "\t * @param sort 정렬 방식 (latest, popular, lowPrice, highPrice)"
	)
	@GetMapping("/search")
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> searchPosts(
		@RequestParam String keyword,
		@RequestParam(required = false) String status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "latest") String sort
	) {
		try {
			if (keyword == null || keyword.trim().isEmpty()) {
				return ResponseEntity.badRequest()
					.body(Map.of(
						"success", false,
						"message", "검색 키워드를 입력해주세요."
					));
			}

			Page<Post> postPage;
			boolean isPopular = "popular".equalsIgnoreCase(sort);

			// status 파라미터가 있으면 해당 상태만 검색, 없으면 전체 검색
			if (status != null && !status.trim().isEmpty()) {
				try {
					Post.PostStatus postStatus = Post.PostStatus.valueOf(status);

					if (isPopular) {
						Pageable pageable = PageRequest.of(page, size);
						postPage = postRepository.searchByKeywordAndStatusByPopularity(keyword.trim(), postStatus, pageable);
					} else {
						Pageable pageable = createPageable(page, size, sort);
						postPage = postRepository.searchByKeywordAndStatus(keyword.trim(), postStatus, pageable);
					}

					log.info("게시글 검색 (status 필터) - keyword: {}, status: {}, page: {}, sort: {}", keyword, status, page, sort);
				} catch (IllegalArgumentException e) {
					return ResponseEntity.badRequest()
						.body(Map.of(
							"success", false,
							"message", "잘못된 판매상태입니다. (판매중, 판매완료 중 선택)"
						));
				}
			} else {
				if (isPopular) {
					Pageable pageable = PageRequest.of(page, size);
					postPage = postRepository.searchByKeywordByPopularity(keyword.trim(), pageable);
				} else {
					Pageable pageable = createPageable(page, size, sort);
					postPage = postRepository.searchByKeyword(keyword.trim(), pageable);
				}

				log.info("게시글 검색 (전체) - keyword: {}, page: {}, sort: {}", keyword, page, sort);
			}

			List<Map<String, Object>> postList = postPage.getContent().stream()
				.map(this::convertPostToMap)
				.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of(
				"success", true,
				"posts", postList,
				"currentPage", postPage.getNumber(),
				"totalPages", postPage.getTotalPages(),
				"totalItems", postPage.getTotalElements(),
				"pageSize", postPage.getSize(),
				"keyword", keyword.trim()
			));

		} catch (Exception e) {
			log.error("게시글 검색 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of(
					"success", false,
					"message", "게시글 검색 실패: " + e.getMessage()
				));
		}
	}
}
