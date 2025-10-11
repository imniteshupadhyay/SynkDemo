package com.playmotech.api.core.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.playmotech.api.core.dto.Response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseController {
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Response<Map<String, String>>> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
		Response.ResponseBuilder<Map<String, String>> responseBody = Response.builder();
		responseBody.body(errors);
		responseBody.status(HttpStatus.BAD_REQUEST.value());
		if (!ex.getBindingResult().getFieldErrors().isEmpty()) {
			responseBody.message(ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
		}
		return ResponseEntity.badRequest().body(responseBody.build());
	}

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Throwable.class)
	public ResponseEntity<Response<String>> handleISE(Throwable ex) {
		log.error("Internal server error", ex);
		Response.ResponseBuilder<String> responseBody = Response.builder();
		responseBody.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
		responseBody.message(ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody.build());
	}
}
