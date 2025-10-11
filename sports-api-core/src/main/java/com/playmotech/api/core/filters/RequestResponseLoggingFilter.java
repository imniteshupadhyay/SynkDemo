package com.playmotech.api.core.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestResponseLoggingFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		// Wrap request and response to allow for repeated reads
		ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(req);
		ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(res);

		// Proceed with the request
		filterChain.doFilter(wrappedRequest, wrappedResponse);

		// Log request and response after the filter chain is done
		String requestId = UUID.randomUUID().toString();
		if (req.getRequestURI() != null && !req.getRequestURI().contains("/sports/actuator/health")) {
			logRequest(wrappedRequest, requestId);
			logResponse(wrappedResponse, requestId);
		}

		// Copy the response body back to the original response after logging
		wrappedResponse.copyBodyToResponse();
	}

	private void logRequest(ContentCachingRequestWrapper request, String requestId) throws IOException {
		String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
		String requestLog = String.format("Request: requestId=%s, method=%s, uri=%s, params=%s, body=%s", requestId,
				request.getMethod(), request.getRequestURI(), request.getQueryString(),
				body.isEmpty() ? "EMPTY" : body);
		log.debug(requestLog);
	}

	private void logResponse(ContentCachingResponseWrapper response, String requestId) throws IOException {
		String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

		String responseLog = String.format("Response: requestId=%s, status=%d, body=%s", requestId,
				response.getStatus(), body.isEmpty() ? "EMPTY" : body);
		log.debug(responseLog);
	}
}
