package org.ssafy.ssafymarket.minio;


import java.io.ByteArrayInputStream;
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

	//이미지 저장
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
