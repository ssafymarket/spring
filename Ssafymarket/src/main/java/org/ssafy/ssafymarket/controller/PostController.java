package org.ssafy.ssafymarket.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.ssafy.ssafymarket.dto.PostCreateRequest;
import org.ssafy.ssafymarket.minio.MinioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Validated
@Slf4j
public class PostController {
	private final MinioService minioService;

	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<Boolean> uploadAndCreatePost(
		@RequestPart("file") @NotNull MultipartFile file,
		@RequestParam("title") @NotNull String title,
		@RequestParam("price") @NotNull Integer price,
		@RequestParam("category")@NotNull String category

	) {

		PostCreateRequest newPost = PostCreateRequest.builder()
			.title(title)
			.price(price)
			.category(category)
			.build();
		try {
			log.info("이미지 저장 TRY ");
			Long postId = minioService.saveImage(file, newPost);
			return ResponseEntity.ok(true);
		}catch (Exception exception){
			return ResponseEntity.ok(false);
		}


	}
}
