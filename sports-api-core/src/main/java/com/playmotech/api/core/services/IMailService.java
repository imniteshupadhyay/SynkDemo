package com.playmotech.api.core.services;

import java.util.Map;

import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.Mail;
import com.playmotech.api.core.exceptions.EmailSendException;
import com.sendgrid.Response;

public interface IMailService {
	/**
	 * Send OTP email to user
	 * 
	 * @param user User profile to send OTP to
	 * @param otp  The OTP to send
	 * @return true if email was sent successfully
	 */
	boolean sendOtpMail(UserProfile user, String otp);

	/**
	 * Send email with template
	 * 
	 * @param mail Mail object containing email details
	 * @return true if email was sent successfully
	 */
	boolean sendMailWithTemplate(Mail mail);

	/**
	 * Send a template email with dynamic data
	 * 
	 * @param to           The recipient email address
	 * @param subject      The email subject
	 * @param templateId   The template ID
	 * @param templateData The dynamic template data
	 * @return The SendGrid API response
	 * @throws EmailSendException If the email cannot be sent
	 */
	Response sendTemplateEmail(String to, String subject, String templateId,
			Map<String, String> templateData) throws EmailSendException;
}
