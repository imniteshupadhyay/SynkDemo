package com.playmotech.api.core.response;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@Component
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ServiceResponse {

	@JsonProperty("status")
	private int status;

	@JsonProperty("message")
	private String message;

	@JsonProperty("httpStatus")
	private HttpStatus httpStatus;

	@JsonProperty("body")
	private Object body;

	@JsonProperty("totalElements")
	private long totalElements;

	@JsonProperty("totalPages")
	private int totalPages;

	@JsonProperty("correlationId")
	private String correlationId;

	public static ServiceResponse setResponse(ServiceResponse response, String message, HttpStatus httpStatus) {
		response.setMessage(message);
		response.setHttpStatus(httpStatus);
		return response;
	}

	public void setHttpStatus(HttpStatus httpStatus) {
		this.status = httpStatus.value();
		this.httpStatus = httpStatus;
	}
}
