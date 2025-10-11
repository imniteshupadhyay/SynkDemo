package com.playmotech.api.core.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.playmotech.api.core.constants.ErrorCodes;

public class ResponseBuilder {

	private ResponseBuilder() {
		super();
	}

	private static ServiceResponse buildResponse(Object body, String message, HttpStatus httpStatus, int totalPages,
			long totalElements) {
		ServiceResponse response = new ServiceResponse();
		response.setBody(body);
		response.setMessage(message);
		response.setHttpStatus(httpStatus);
		response.setTotalPages(totalPages);
		response.setTotalElements(totalElements);
		return response;
	}

	public static ServiceResponse buildResponse(Object body, String message, HttpStatus httpStatus) {
		ServiceResponse response = new ServiceResponse();
		response.setBody(body);
		response.setMessage(message);
		response.setHttpStatus(httpStatus);
		return response;
	}

	public static ServiceResponse success(ApiResponse response) {
		return buildResponse(null, response.getMessage(), HttpStatus.OK);
	}

	public static ServiceResponse success(Object body, ApiResponse response, int totalPages, long totalElements) {
		return buildResponse(body, response.getMessage(), HttpStatus.OK, totalPages, totalElements);
	}

	public static ServiceResponse success(Object body, ApiResponse response) {
		return buildResponse(body, response.getMessage(), HttpStatus.OK);
	}

	public static ServiceResponse success(Object body, ApiResponse response, HttpStatus httpStatus) {
		return buildResponse(body, response.getMessage(), httpStatus);
	}

	public static ServiceResponse success(ApiResponse response, HttpStatus httpStatus) {
		return buildResponse(null, response.getMessage(), httpStatus);
	}

	public static ServiceResponse success(Object body, ApiResponse response, HttpStatus httpStatus, int totalPages,
			long totalElements) {
		return buildResponse(body, response.getMessage(), httpStatus, totalPages, totalElements);
	}

	public static ServiceResponse error(Object body, ApiResponse response, HttpStatus httpStatus) {
		return buildResponse(body, response.getMessage(), httpStatus);
	}

	public static ServiceResponse error(ApiResponse response, HttpStatus httpStatus) {
		return buildResponse(null, response.getMessage(), httpStatus);
	}

	public static ServiceResponse error(String errorMessage, HttpStatus httpStatus) {
		return buildResponse(null, errorMessage, httpStatus);
	}

	// Error Response Methods for ResponseEntity
	private static ResponseEntity<ServiceResponse> errorResponseEntity(String errorMessage, HttpStatus httpStatus) {
		return new ResponseEntity<>(buildResponse(null, errorMessage, httpStatus), httpStatus);
	}

	private static ResponseEntity<ServiceResponse> errorResponseEntity(ApiResponse response, HttpStatus httpStatus) {
		return new ResponseEntity<>(buildResponse(null, response.getMessage(), httpStatus), httpStatus);
	}

	// Error Response Methods for ServiceResponse (non-ResponseEntity)
	private static ServiceResponse errorResponse(String errorMessage, HttpStatus httpStatus) {
		return buildResponse(null, errorMessage, httpStatus);
	}

	private static ServiceResponse errorResponse(ApiResponse response, HttpStatus httpStatus) {
		return buildResponse(null, response.getMessage(), httpStatus);
	}

	// Specific Error Responses for ResponseEntity
	public static ResponseEntity<ServiceResponse> badRequestEntity(String errorMessage) {
		return errorResponseEntity(errorMessage, HttpStatus.BAD_REQUEST);
	}

	public static ResponseEntity<ServiceResponse> badRequestEntity(ApiResponse response) {
		return errorResponseEntity(response, HttpStatus.BAD_REQUEST);
	}

	public static ResponseEntity<ServiceResponse> notFoundEntity(String errorMessage) {
		return errorResponseEntity(errorMessage, HttpStatus.NOT_FOUND);
	}

	public static ResponseEntity<ServiceResponse> forbiddenEntity(String errorMessage) {
		return errorResponseEntity(errorMessage, HttpStatus.FORBIDDEN);
	}

	public static ResponseEntity<ServiceResponse> unauthorizedEntity(String errorMessage) {
		return errorResponseEntity(errorMessage, HttpStatus.UNAUTHORIZED);
	}

	public static ResponseEntity<ServiceResponse> internalServerErrorEntity(String errorMessage) {
		return errorResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	// Specific Error Responses for ServiceResponse (non-ResponseEntity)
	public static ServiceResponse badRequest(String errorMessage) {
		return errorResponse(errorMessage, HttpStatus.BAD_REQUEST);
	}

	public static ServiceResponse badRequest(ApiResponse response) {
		return errorResponse(response, HttpStatus.BAD_REQUEST);
	}

	public static ServiceResponse notFound(String errorMessage) {
		return errorResponse(errorMessage, HttpStatus.NOT_FOUND);
	}

	public static ServiceResponse notFound(ApiResponse response) {
		return errorResponse(response, HttpStatus.NOT_FOUND);
	}

	public static ServiceResponse notAcceptable(String errorMessage) {
		return errorResponse(errorMessage, HttpStatus.NOT_ACCEPTABLE);
	}

	public static ServiceResponse notAcceptable(ApiResponse response) {
		return errorResponse(response, HttpStatus.NOT_ACCEPTABLE);
	}

	public static ServiceResponse forbidden(String errorMessage) {
		return errorResponse(errorMessage, HttpStatus.FORBIDDEN);
	}

	public static ServiceResponse unauthorized(String errorMessage) {
		return errorResponse(errorMessage, HttpStatus.UNAUTHORIZED);
	}

	public static ServiceResponse internalServerError(String errorMessage) {
		return errorResponse(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public static ServiceResponse internalServerError(ApiResponse response) {
		return errorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public static ServiceResponse conflict(ApiResponse response) {
		return errorResponse(response, HttpStatus.CONFLICT);
	}

	public static ServiceResponse conflict(String response) {
		return errorResponse(response, HttpStatus.CONFLICT);
	}

	public static ServiceResponse success(Object responseData, String message) {
		return buildResponse(responseData, message, HttpStatus.OK);
	}

	public static ServiceResponse error(Object responseData, String message, HttpStatus status) {
		return buildResponse(responseData, message, status);
	}

	public static ServiceResponse error(Object errorBody, HttpStatus badRequest) {
		return buildResponse(errorBody, "Failed", badRequest);
	}

	public static ServiceResponse error(String message, ErrorCodes errorCode, HttpStatus httpStatus) {
		ServiceResponse response = new ServiceResponse();
		response.setMessage(message);
		response.setHttpStatus(httpStatus);
		response.setStatus(errorCode.getCustomError());
		return response;
	}

	public static ServiceResponse success(String message) {
		return success(message, HttpStatus.OK);
	}

	public static ServiceResponse success(String message, HttpStatus httpStatus) {
		ServiceResponse response = new ServiceResponse();
		response.setMessage(message);
		response.setHttpStatus(httpStatus);
		return response;
	}

	public static ServiceResponse success(Object body, String message, HttpStatus httpStatus) {
		ServiceResponse response = new ServiceResponse();
		response.setBody(body);
		response.setMessage(message);
		response.setHttpStatus(httpStatus);
		return response;
	}

}
