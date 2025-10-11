package com.playmotech.api.core.response.excel;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ErrorReportEntry {
	private int rowNumber;
	private List<String> originalData;
	private List<String> errorMessages;
}