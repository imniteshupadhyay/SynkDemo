package com.playmotech.api.core.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {

	public static String hashPasswordWithSHA512(String tempPassword) {
		try {
			StringBuilder sb = new StringBuilder();
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			byte[] bytes = md.digest(tempPassword.getBytes());

			for (byte b : bytes) {
				sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
			}

			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-512 algorithm not found", e);
		}
	}

	public static String generateDefaultPassword(String phoneNumber, String name) {
		return "PlayMo@123";
	}

//	public String generateDefaultPassword(String phoneNumber, String name) {
//	if (name == null || phoneNumber == null) {
//		return "PlayMo@123"; // Fallback password (compliant with all rules)
//	}
//
//	// Clean and extract name
//	String cleanedName;
//	String trimmedName = name.trim();
//
//	if (trimmedName.isEmpty()) {
//		cleanedName = "Usr"; // Default if name is empty after trimming
//	} else if (trimmedName.contains(" ")) {
//		// Name contains space - get first part
//		String firstPart = trimmedName.split("\\s+")[0];
//
//		// Format name with first letter uppercase, rest lowercase
//		firstPart = firstPart.substring(0, 1).toUpperCase()
//				+ (firstPart.length() > 1 ? firstPart.substring(1).toLowerCase() : "");
//
//		// Apply length rules
//		if (firstPart.length() <= 10) {
//			cleanedName = firstPart;
//		} else {
//			cleanedName = firstPart.substring(0, 3);
//		}
//	} else {
//		// No space in name - use entire name
//		String singleName = trimmedName;
//
//		// Format name with first letter uppercase, rest lowercase
//		singleName = singleName.substring(0, 1).toUpperCase()
//				+ (singleName.length() > 1 ? singleName.substring(1).toLowerCase() : "");
//
//		// Apply length rules
//		if (singleName.length() <= 10) {
//			cleanedName = singleName;
//		} else {
//			cleanedName = singleName.substring(0, 3);
//		}
//	}
//
//	// Ensure name is at least 3 characters
//	if (cleanedName.length() < 3) {
//		cleanedName = String.format("%-3s", cleanedName).replace(' ', 'x');
//	}
//
//	// Extract last 5 digits from phone number
//	String digits = phoneNumber.replaceAll("\\D", "");
//	String numberPart;
//
//	if (digits.isEmpty()) {
//		numberPart = "00000";
//	} else if (digits.length() >= 5) {
//		numberPart = digits.substring(digits.length() - 5);
//	} else {
//		numberPart = String.format("%05d", Integer.parseInt(digits));
//	}
//
//	// Build the password: [Name]@[Number]
//	String password = cleanedName + "@" + numberPart;
//
//	return password;
//}

}
