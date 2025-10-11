package com.playmotech.api.core.config;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.playmotech.api.core.services.IStorageService;

import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
@Configuration
@ConfigurationProperties(prefix = "fcm")
public class GoogleFCMConfig {

	private String s3Bucket;
	private String s3File;

	@Bean
	FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
		return FirebaseMessaging.getInstance(firebaseApp);
	}

	@Bean
	FirebaseApp firebaseApp(GoogleCredentials credentials) {
		FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();

		return FirebaseApp.initializeApp(options);
	}

	@Bean
	GoogleCredentials googleCredentials(IStorageService storageService) {
//        try (InputStream serviceAccount =
//                    this.getClass().getClassLoader().getResourceAsStream("letsrally-41556-d59b08dda31e.json")) {
//            if (serviceAccount == null) throw new RuntimeException("FCM Service Account credential file not found.");
//            return GoogleCredentials.fromStream(serviceAccount);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
		try (InputStream serviceAccount = storageService.readInputStream(s3Bucket, s3File)) {
			if (serviceAccount == null) {
				throw new RuntimeException("FCM Service Account credential file not found.");
			}
			return GoogleCredentials.fromStream(serviceAccount);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}