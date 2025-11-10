package org.ssafy.ssafymarket.minio;


import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.ssafy.ssafymarket.config.MinioProperties;
import org.ssafy.ssafymarket.dto.PostCreateRequest;
import org.ssafy.ssafymarket.entity.Post;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.PostRepository;
import org.ssafy.ssafymarket.repository.UserRepository;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

	private final MinioClient minioClient;
	private final PostRepository postRepository;
	private final UserRepository userRepository;

	@Value("${minio.bucket}")
	private String bucketName;

	@PostConstruct
	public void ensureBucket() throws Exception {
		boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
		if (!exists) {
			minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
			log.info("Created MinIO bucket: {}", bucketName);
		} else {
			log.info("MinIO bucket exists: {}", bucketName);
		}
	}

	/**
	 * 여러 이미지를 저장하고 게시글 생성
	 * @param files 업로드할 이미지 파일 리스트
	 * @param newPost 게시글 정보
	 * @return 생성된 게시글 ID
	 */
	@Transactional
	public Long saveImages(List<MultipartFile> files, PostCreateRequest newPost) {
		log.info("여러 이미지 저장 시작 - 파일 개수: {}", files.size());

		// 최대 10개 이미지 제한
		if (files.size() > 10) {
			throw new IllegalArgumentException("이미지는 최대 10개까지 업로드 가능합니다.");
		}

		// 빈 파일 체크
		if (files.isEmpty()) {
			throw new IllegalArgumentException("최소 1개의 이미지가 필요합니다.");
		}

		// 인증된 사용자 확인
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String studentId = auth.getName();
		log.info("업로드 사용자: {}", studentId);

		User writer = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + studentId));

		// Post 엔티티 생성
		Post post = Post.builder()
				.title(newPost.getTitle())
				.price(newPost.getPrice())
				.category(newPost.getCategory())
				.description(newPost.getDescription())
				.writer(writer)
				.build();

		// MinIO에 업로드된 이미지 URL 리스트 (보상 삭제용)
		List<String> uploadedObjectNames = new ArrayList<>();

		try {
			// 각 파일을 MinIO에 업로드
			for (int i = 0; i < files.size(); i++) {
				MultipartFile file = files.get(i);
				String objectName = uploadToMinio(file);
				uploadedObjectNames.add(objectName);

				// Post에 이미지 추가 (순서대로)
				String imageUrl = bucketName + "/" + objectName;
				post.addImage(imageUrl);

				log.info("이미지 업로드 완료 [{}/{}]: {}", i + 1, files.size(), objectName);
			}

			// DB에 저장 (Post와 PostImage 모두 저장됨 - CascadeType.ALL)
			Post savedPost = postRepository.save(post);
			log.info("게시글 저장 완료 - postId: {}, 이미지 개수: {}", savedPost.getPostId(), savedPost.getImages().size());

			return savedPost.getPostId();

		} catch (Exception ex) {
			// DB 저장 실패 시 MinIO 보상 삭제
			log.error("게시글 저장 실패, MinIO 보상 삭제 시작", ex);
			compensateMinioUpload(uploadedObjectNames);
			throw new RuntimeException("게시글 저장 실패: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 단일 이미지 저장 (하위 호환성을 위해 유지)
	 * @deprecated saveImages() 사용 권장
	 */
	@Deprecated
	@Transactional
	public Long saveImage(MultipartFile file, PostCreateRequest newPost){
		log.info("이미지 저장 초입");
		String contentType = safeContentType(file.getContentType());
		String ext = extFromContentTypeOrName(contentType, file.getOriginalFilename());
		String objectName = "images/%s.%s".formatted(UUID.randomUUID(), ext);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String studentId = auth.getName(); // username을 studentId로 저장했을 경우

		log.info(studentId);

		User writer = userRepository.findByStudentId(studentId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + studentId));

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (Exception e) {
			throw new RuntimeException("파일 읽기 실패", e);
		}

		//업로드
		try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
			PutObjectArgs args = PutObjectArgs.builder()
				.bucket(bucketName)
				.object(objectName)
				.stream(is, bytes.length, -1)
				.contentType(contentType)
				.build();
			minioClient.putObject(args);
			log.info("Saved image to MinIO: {}", objectName);
		} catch (Exception e) {
			log.error("MinIO 업로드 실패", e);
			throw new RuntimeException("MinIO 업로드 실패", e);
		}

		//db 저장
		try{
			Post post= Post.builder()
				.price(newPost.getPrice())
				.category(newPost.getCategory())
				.title(newPost.getTitle())
				.imageUrl(bucketName + "/" + objectName)
				.writer(writer)

				.build();

			Post saved=postRepository.save(post);

			return saved.getPostId();
		} catch (Exception dbEx) {
			// 4) DB 실패 시 MinIO 보상 삭제
			try {
				minioClient.removeObject(RemoveObjectArgs.builder()
					.bucket(bucketName)
					.object(objectName)
					.build());
				log.warn("DB 실패로 MinIO 객체 보상 삭제: {}", objectName);
			} catch (Exception rmEx) {
				log.error("보상 삭제 실패: {}", objectName, rmEx);
			}
			throw dbEx; // 트랜잭션 롤백
		}


	}

	/**
	 * 채팅 이미지 업로드 (Post와 무관한 단독 이미지)
	 * @param file 업로드할 이미지 파일
	 * @return 전체 이미지 URL (bucket/objectName 형식)
	 */
	public String uploadChatImage(MultipartFile file) {
		log.info("채팅 이미지 업로드 시작 - 파일명: {}", file.getOriginalFilename());

		// 파일 검증
		if (file.isEmpty()) {
			throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
		}

		// 파일 크기 제한 (10MB)
		if (file.getSize() > 10 * 1024 * 1024) {
			throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
		}

		// MinIO 업로드
		String objectName = uploadToMinio(file);
		String imageUrl = bucketName + "/" + objectName;

		log.info("채팅 이미지 업로드 완료: {}", imageUrl);
		return imageUrl;
	}

	/**
	 * 파일을 MinIO에 업로드하고 objectName 반환
	 * 모든 이미지는 images/ 폴더에 저장
	 * @param file 업로드할 파일
	 * @return MinIO objectName (예: images/uuid.jpg)
	 */
	private String uploadToMinio(MultipartFile file) {
		String contentType = safeContentType(file.getContentType());
		String ext = extFromContentTypeOrName(contentType, file.getOriginalFilename());
		String objectName = "images/%s.%s".formatted(UUID.randomUUID(), ext);

		try {
			byte[] bytes = file.getBytes();
			try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
				PutObjectArgs args = PutObjectArgs.builder()
						.bucket(bucketName)
						.object(objectName)
						.stream(is, bytes.length, -1)
						.contentType(contentType)
						.build();
				minioClient.putObject(args);
				log.debug("MinIO 업로드 성공: {}", objectName);
				return objectName;
			}
		} catch (Exception e) {
			log.error("MinIO 업로드 실패: {}", objectName, e);
			throw new RuntimeException("MinIO 업로드 실패: " + e.getMessage(), e);
		}
	}

	/**
	 * MinIO 업로드 보상 삭제 (DB 저장 실패 시 롤백용)
	 * @param objectNames 삭제할 objectName 리스트
	 */
	private void compensateMinioUpload(List<String> objectNames) {
		for (String objectName : objectNames) {
			try {
				minioClient.removeObject(RemoveObjectArgs.builder()
						.bucket(bucketName)
						.object(objectName)
						.build());
				log.warn("MinIO 보상 삭제 완료: {}", objectName);
			} catch (Exception e) {
				log.error("MinIO 보상 삭제 실패: {}", objectName, e);
			}
		}
	}

	private String safeContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}
		return contentType;
	}

	private String extFromContentTypeOrName(String contentType, String originalFilename) {
		if (originalFilename != null && originalFilename.contains(".")) {
			return originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
		}
		return switch (contentType) {
			case "image/png" -> "png";
			case "image/jpeg" -> "jpg";
			case "image/gif" -> "gif";
			case "image/webp" -> "webp";
			default -> "bin";
		};
	}

}
