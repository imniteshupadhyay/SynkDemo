package com.playmotech.api.core.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
	private List<T> body;
	private long totalElements;
	private int totalPages;
	private int currentPage;
}
