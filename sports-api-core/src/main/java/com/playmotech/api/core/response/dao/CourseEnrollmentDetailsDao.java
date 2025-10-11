package com.playmotech.api.core.response.dao;

import lombok.Data;

@Data
public class CourseEnrollmentDetailsDao {

	private String enrollmentId;
	private String traineeUserId;
	private String academyId;
	private String branchId;
	private String courseId;
	private String sport;
	private String paymentSchedule;
	private Long totalCourseFeeCollected;
	private Long totalRegistrationFeeCollected;
	private Long pendingCourseFee;
	private Long pendingRegistrationFee;

}
