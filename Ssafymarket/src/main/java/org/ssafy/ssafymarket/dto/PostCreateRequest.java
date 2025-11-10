package org.ssafy.ssafymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {

	private String title;       // 제목
	private Integer price;      // 가격
	private String category;    // 카테고리
	private String description; // 상품 설명
	private String imageUrl;    // 업로드된 이미지 경로 (MinIO 업로드 후 설정)
}

