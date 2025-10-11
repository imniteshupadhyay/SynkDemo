package com.playmotech.api.core.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Created By: deep.patel
 **/

@Getter
@Builder
public class Response<T> {
	private final int status;
	private final String message;
	private final T body;
	private final int totalPages;
	private final long totalElements;
}
