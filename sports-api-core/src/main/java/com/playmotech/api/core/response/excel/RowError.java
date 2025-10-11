package com.playmotech.api.core.response.excel;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RowError {
	private int rowNumber;
	private List<String> messages;
}