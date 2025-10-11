package com.playmotech.api.core.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.config.SendGridConfig;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dao_postgres.OrganisationConfig;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.Mail;
import com.playmotech.api.core.exceptions.EmailSendException;
import com.playmotech.api.core.repo.OrgConfigRepo;
import com.playmotech.api.core.repo.OrgRepo;
import com.playmotech.api.core.services.IMailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService implements IMailService {

	private static final String HEADER_KEY = "header";
	private static final String FOOTER_KEY = "footer";
	private static final String COMPANY_NAME = "PlayMo";
	private static final String OTP_SUBJECT_SUFFIX = " - OTP for Login Verification";
	private static final String TEXT_HTML = "text/html";
	private static final String TEXT_PLAIN = "text/plain";
	private static final String OTP_TEMPLATE_ID_KEY = "otpTemplateId";

	private final SendGrid sendGrid;
	private final SendGridConfig sendGridConfig;
	private final HttpServletRequest requestContext;
	private final OrgRepo orgRepo;
	private final OrgConfigRepo orgConfigRepo;

	@Value("${sendgrid.template.otp-default}")
	private String defaultOtpTemplateId; // The fallback value must be default otp template id (SendGrid)

	@Override
	@Retryable(retryFor = { EmailSendException.class }, maxAttemptsExpression = "${sendgrid.retry.max-attempts}")
	public boolean sendOtpMail(UserProfile user, String otp) {
		try {
			String otpTemplateId = defaultOtpTemplateId;
			log.info("OTP template id at the start of sendOtpMail: {}", otpTemplateId);
			Map<String, String> templateData = new HashMap<>();
			templateData.put(HEADER_KEY, "Login Verification");
			templateData.put("otp", otp);
			templateData.put(FOOTER_KEY, COMPANY_NAME);
			templateData.put("displayName", user.getDisplayName());
			templateData.put("email", user.getEmailId());
			templateData.put("companyName", COMPANY_NAME);

			String subject = COMPANY_NAME + OTP_SUBJECT_SUFFIX;

			String domainUrl = requestContext.getHeader("origin");
			if (StringUtils.hasText(domainUrl)) {
				Optional<Organisation> org = orgRepo.findByDomainUrlIgnoreCase(domainUrl);
				if (org.isPresent()) {
					Optional<OrganisationConfig> config = orgConfigRepo.findByOrgIdAndKey(org.get().getId(),
							OTP_TEMPLATE_ID_KEY);
					if (config.isPresent()) {
						otpTemplateId = StringUtils.hasText(config.get().getValue()) ? config.get().getValue()
								: otpTemplateId; // fallback to default otp template id
						log.info("OTP template id if config is present: {}", otpTemplateId);
					}
				}
			}
			log.info("OTP template id after org config check: {}", otpTemplateId);
			if (otpTemplateId != null && !otpTemplateId.isEmpty()) {
				// Use SendGrid dynamic template
				sendTemplateEmail(user.getEmailId(), subject, otpTemplateId, templateData);
			} else {
				// Fallback to simple email with template data
				sendSimpleEmail(user.getEmailId(), subject, templateData);
			}

			return true;
		} catch (Exception e) {
			log.error("Failed to send OTP email to {}: {}", user.getEmailId(), e.getMessage());
			throw new EmailSendException("Failed to send OTP email", e);
		}
	}

	@Override
	@Retryable(retryFor = { EmailSendException.class }, maxAttemptsExpression = "${sendgrid.retry.max-attempts}")
	public boolean sendMailWithTemplate(Mail mail) {
		try {
			com.sendgrid.helpers.mail.Mail sgMail = new com.sendgrid.helpers.mail.Mail();
			Email from = new Email(mail.getSender() != null ? mail.getSender() : sendGridConfig.getFromEmail(),
					sendGridConfig.getFromName());
			sgMail.setFrom(from);
			sgMail.setSubject(mail.getSubject());

			Personalization personalization = new Personalization();
			personalization.addTo(new Email(mail.getRecipient()));

			// Add CC if present
			if (mail.getCcRecipient() != null && !mail.getCcRecipient().isEmpty()) {
				personalization.addCc(new Email(mail.getCcRecipient()));
			}

			// Add BCC if present
			if (mail.getBccRecipient() != null && !mail.getBccRecipient().isEmpty()) {
				personalization.addBcc(new Email(mail.getBccRecipient()));
			}

			// Add dynamic template data if present
			if (mail.getModel() != null) {
				mail.getModel().forEach((key, value) -> {
					if (value != null) {
						personalization.addDynamicTemplateData(key, value.toString());
					}
				});
			}

			sgMail.addPersonalization(personalization);

			if (!mail.getAttachments().isEmpty()) {
				for (String attachmentUrl : mail.getAttachments()) {
					try {
						Attachments sgAttachment = createAttachmentFromUrl(attachmentUrl);
						if (sgAttachment != null) {
							sgMail.addAttachments(sgAttachment);
						}
					} catch (Exception e) {
						log.warn("Failed to add attachment from URL: {} - Error - {}", attachmentUrl, e.getMessage());
					}
				}
			}

			// If template ID is provided, use it, otherwise use content
			if (mail.getTemplateName() != null && !mail.getTemplateName().isEmpty()) {
				sgMail.setTemplateId(mail.getTemplateName());
			} else {
				String content = mail.getMessage();

				if (StringUtils.hasText(content)) {
					// Check if content contains HTML tags, if not treat as plain text
					if (content.contains("<") && content.contains(">")) {
						sgMail.addContent(new Content(TEXT_HTML, content));
					} else {
						sgMail.addContent(new Content(TEXT_PLAIN, content));
					}
				} else {
					log.warn("No content provided for email to: {}", mail.getRecipient());
					sgMail.addContent(new Content(TEXT_PLAIN, ""));
				}
			}

			return sendEmail(sgMail, mail.getRecipient());
		} catch (Exception e) {
			log.error("Error sending email to {}: {}", mail.getRecipient(), e.getMessage());
			throw new EmailSendException("Failed to send email with template", e);
		}
	}

	private Attachments createAttachmentFromUrl(String attachmentUrl) throws IOException {
		if (!StringUtils.hasText(attachmentUrl)) {
			return null;
		}
		try {
			// Download file from URL
			byte[] fileContent = downloadFileFromUrl(attachmentUrl);
			if (fileContent == null || fileContent.length == 0) {
				log.warn("No content downloaded from URL: {}", attachmentUrl);
				return null;
			}

			// Extract filename from URL
			String filename = extractFilenameFromUrl(attachmentUrl);

			// Encode file content to Base64
			String encodedContent = Base64.getEncoder().encodeToString(fileContent);

			// Determine content type
			String contentType = determineContentType(filename);

			// Create SendGrid attachment
			Attachments attachment = new Attachments();
			attachment.setContent(encodedContent);
			attachment.setType(contentType);
			attachment.setFilename(filename);
			attachment.setDisposition("attachment");

			log.debug("Created attachment: {} with size: {} bytes", filename, fileContent.length);
			return attachment;

		} catch (Exception e) {
			log.error("Error creating attachment from URL: {} - Error: {}", attachmentUrl, e.getMessage());
			throw new IOException("Failed to create attachment from URL: " + attachmentUrl, e);
		}
	}

	@Override
	@Retryable(retryFor = { EmailSendException.class }, maxAttemptsExpression = "${sendgrid.retry.max-attempts}")
	public Response sendTemplateEmail(String to, String subject, String templateId, Map<String, String> templateData)
			throws EmailSendException {
		try {
			com.sendgrid.helpers.mail.Mail mail = new com.sendgrid.helpers.mail.Mail();
			mail.setFrom(new Email(sendGridConfig.getFromEmail(), sendGridConfig.getFromName()));
			mail.setSubject(subject);
			mail.setTemplateId(templateId);

			Personalization personalization = new Personalization();
			personalization.addTo(new Email(to));

			if (templateData != null) {
				templateData.forEach(personalization::addDynamicTemplateData);
			}

			mail.addPersonalization(personalization);

			Request request = new Request();
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());

			Response response = sendGrid.api(request);
			logResponse(response, to);

			if (response.getStatusCode() >= 400) {
				throw new EmailSendException(String.format("Failed to send email. Status: %d, Body: %s",
						response.getStatusCode(), response.getBody()));
			}

			return response;
		} catch (IOException e) {
			log.error("Error sending template email to {}: {}", to, e.getMessage());
			throw new EmailSendException("Failed to send template email", e);
		}
	}

	/**
	 * Send a simple email with the given parameters
	 * 
	 * @param to           Recipient email address
	 * @param subject      Email subject
	 * @param templateData Map of template data
	 * @return true if email was sent successfully
	 * @throws EmailSendException if email sending fails
	 */
	private boolean sendSimpleEmail(String to, String subject, Map<String, ?> templateData) throws EmailSendException {
		try {
			com.sendgrid.helpers.mail.Mail mail = new com.sendgrid.helpers.mail.Mail();
			mail.setFrom(new Email(sendGridConfig.getFromEmail(), sendGridConfig.getFromName()));
			mail.setSubject(subject);
			Personalization personalization = new Personalization();
			personalization.addTo(new Email(to));
			mail.addPersonalization(personalization);

			// Build simple HTML content from template data
			StringBuilder content = new StringBuilder("<html><body>");
			content.append("<h1>").append(subject).append("</h1>");

			if (templateData != null) {
				templateData.forEach((key, value) -> content.append("<p><strong>").append(key).append(":</strong> ")
						.append(value).append("</p>"));
			}

			content.append("</body></html>");

			mail.addContent(new Content(TEXT_HTML, content.toString()));

			return sendEmail(mail, to);
		} catch (Exception e) {
			log.error("Error sending simple email to {}: {}", to, e.getMessage());
			throw new EmailSendException("Failed to send simple email", e);
		}
	}

	/**
	 * Send email using SendGrid API
	 * 
	 * @param mail      The SendGrid mail object
	 * @param recipient The recipient email address (for logging)
	 * @return true if email was sent successfully
	 * @throws EmailSendException if email sending fails
	 */
	private boolean sendEmail(com.sendgrid.helpers.mail.Mail mail, String recipient) throws EmailSendException {
		try {
			Request request = new Request();
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());

			Response response = sendGrid.api(request);
			logResponse(response, recipient);

			if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
				throw new EmailSendException(String.format("Failed to send email. Status: %d, Body: %s",
						response.getStatusCode(), response.getBody()));
			}

			return true;
		} catch (IOException e) {
			log.error("Error sending email to {}: {}", recipient, e.getMessage());
			throw new EmailSendException("Failed to send email", e);
		}
	}

	/**
	 * Log the response from SendGrid
	 * 
	 * @param response  The SendGrid response
	 * @param recipient The recipient email address
	 */
	private void logResponse(Response response, String recipient) {
		if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
			log.info("Email sent successfully to {}. Status: {}", recipient, response.getStatusCode());
		} else {
			log.warn("Failed to send email to {}. Status: {}, Body: {}", recipient, response.getStatusCode(),
					response.getBody());
		}
	}

	private byte[] downloadFileFromUrl(String fileUrl) throws IOException {
		try {
			String encodedUrl = encodeUrl(fileUrl);

			URL url = new URL(encodedUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			// Set proper headers for better compatibility
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
			connection.setRequestProperty("Accept", "*/*");
			connection.setRequestProperty("Connection", "close");

			// Set timeouts
			connection.setConnectTimeout(10000); // 10 seconds
			connection.setReadTimeout(30000); // 30 seconds

			// Check response code
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				throw new IOException("HTTP " + responseCode + " error downloading file from: " + fileUrl);
			}

			try (InputStream inputStream = connection.getInputStream();
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

				byte[] buffer = new byte[8192];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}

				return outputStream.toByteArray();
			} finally {
				connection.disconnect();
			}

		} catch (Exception e) {
			log.error("Failed to download file from URL: {} - Error: {}", fileUrl, e.getMessage());
			throw new IOException("Failed to download file from URL: " + fileUrl, e);
		}
	}

	private String encodeUrl(String url) {
		try {
			// Parse the URL to handle encoding properly
			URL originalUrl = new URL(url);
			URI uri = new URI(originalUrl.getProtocol(), originalUrl.getHost(), originalUrl.getPath(),
					originalUrl.getQuery(), null);
			return uri.toString();
		} catch (Exception e) {
			log.warn("Could not encode URL: {}, using original", url);
			return url;
		}
	}

	private String extractFilenameFromUrl(String url) {
		try {
			String path = new URL(url).getPath();
			String filename = path.substring(path.lastIndexOf('/') + 1);

			// If no filename found, generate a default one
			if (filename.isEmpty() || !filename.contains(".")) {
				filename = "attachment_" + System.currentTimeMillis() + ".pdf";
			}

			return filename;
		} catch (Exception e) {
			log.warn("Could not extract filename from URL: {}, using default", url);
			return "attachment_" + System.currentTimeMillis() + ".pdf";
		}
	}

	private String determineContentType(String filename) {
		if (filename == null) {
			return "application/octet-stream";
		}

		String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

		switch (extension) {
			case "pdf":
				return "application/pdf";
			case "doc":
				return "application/msword";
			case "docx":
				return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
			case "xls":
				return "application/vnd.ms-excel";
			case "xlsx":
				return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
			case "txt":
				return "text/plain";
			case "csv":
				return "text/csv";
			case "jpg":
			case "jpeg":
				return "image/jpeg";
			case "png":
				return "image/png";
			case "gif":
				return "image/gif";
			case "zip":
				return "application/zip";
			default:
				return "application/octet-stream";
		}
	}
}
