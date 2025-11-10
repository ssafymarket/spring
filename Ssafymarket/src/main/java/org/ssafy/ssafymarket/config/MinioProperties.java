package org.ssafy.ssafymarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
	String endpoint,
	String accessKey,
	String secretKey,
	String bucket,
	boolean secure,
	boolean public_,
	int presignedExpirySeconds
) {}