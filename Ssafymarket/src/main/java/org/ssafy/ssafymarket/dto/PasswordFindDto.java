package org.ssafy.ssafymarket.dto;

import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PasswordFindDto {
	private String studentId;

	private String name;

	private String className;


}
