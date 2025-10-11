package com.playmotech.api.core.agora;

import org.springframework.stereotype.Component;

import com.playmotech.api.core.config.AgoraConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RtcTokenProvider {

	private final AgoraConfig agoraConfig;

	public RtcTokenProvider(AgoraConfig agoraConfig) {
		this.agoraConfig = agoraConfig;
	}

//    static String appId = "fbd6632800bb49c4a4dc7838cb26fd32";
//    static String appCertificate = "1f8a57d50a72456d81f61b16e96e7474";

	public String generateToken(String channelName, int uid) {
		RtcTokenBuilder2 token = new RtcTokenBuilder2();
		return token.buildTokenWithUid(agoraConfig.getAppId(), agoraConfig.getAppCertificate(), channelName, uid,
				RtcTokenBuilder2.Role.ROLE_SUBSCRIBER, agoraConfig.getTokenTtl(), agoraConfig.getPriviledgeTtl());
	}
}
