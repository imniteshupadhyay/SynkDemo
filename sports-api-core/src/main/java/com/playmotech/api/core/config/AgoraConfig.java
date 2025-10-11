package com.playmotech.api.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@Data
@ConfigurationProperties(prefix = "agora")
public class AgoraConfig {
	private String appId;
	private String appCertificate;
	private Integer tokenTtl;
	private Integer priviledgeTtl;

}
