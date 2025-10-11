package com.playmotech.api.core.response.dao;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CoachPerformanceReportViewDao {

	private String reportTitle;

	private String reportJson;

	private String coachName;

	private String playerName;

	private String sport;

	private String academy;

	private String program;

	private String reportStatus;

	private LocalDateTime createdOn;

}
