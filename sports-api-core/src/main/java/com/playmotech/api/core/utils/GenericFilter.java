package com.playmotech.api.core.utils;

import java.sql.Timestamp;
import java.util.List;

import com.playmotech.api.core.constants.RankLevel;
import com.playmotech.api.core.constants.MatchStatus;
import com.playmotech.api.core.constants.TournamentStatus;
import com.playmotech.api.core.dao_postgres.Activity;
import com.playmotech.api.core.dao_postgres.Assessment;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory;
import com.playmotech.api.core.dao_postgres.Leads;
import com.playmotech.api.core.dao_postgres.NewSchedule;
import com.playmotech.api.core.dao_postgres.PaymentReminderConfig;
import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dao_postgres.VideoAnalyzer;
import com.playmotech.api.core.views.CourseDetailsView;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericFilter {
	private Short currentPage;
	private Short pageSize;
	private boolean isPageable;
	private boolean ascending;
	private String orderBy;
	private String apiCalledFrom;
	private String search;
	private String userId;
	private List<String> academyIds;
	private List<String> playerIds;
	private List<String> coachIds;
	private String academyId;
	private String branchId;
	private String programId;
	private String coachId;
	private List<String> branchIds;
	private List<String> programIds;
	private List<String> sports;
	private List<String> ageCategory;
	private List<String> gender;
	private String paymentCategory;

	private Timestamp startDate;
	private Timestamp endDate;

	private String domainUrl;

	private String sport;
	private RankLevel ranking;

	private Integer days;
	private boolean notDeleted;
	private boolean export;
	private String userRole;
	private boolean app;

	private Leads leads;
	private VideoAnalyzer analyzer;
	private String analysisStatus;
	private Trial trial;
	private CourseDetailsView courseView;
	private Activity activity;
	private NewSchedule schedule;
	private PaymentReminderConfig paymentReminderConfig;
	private BulkUploadHistory uploadHistory;

	private String tournamentId;
	private MatchStatus matchStatus;
	private TournamentStatus tournamentStatus;

	private Assessment assessment;
	private AssessmentPlayerRegistration assessmentPlayerRegistration;
	private AssessmentPlayerSubmission assessmentPlayerSubmission;

}
