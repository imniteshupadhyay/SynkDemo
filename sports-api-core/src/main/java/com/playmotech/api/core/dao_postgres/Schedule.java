package com.playmotech.api.core.dao_postgres;

import com.playmotech.api.core.constants.ScheduleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "schedules")
@Data
@ToString(exclude = "course")
public class Schedule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "course_id", referencedColumnName = "id")
	private Course course;

	@Column(name = "type")
	@Enumerated(value = EnumType.STRING)
	private ScheduleType type;

	@Column(name = "custom_dates_json")
	private String customDatesJson;

	@Column(name = "weekdays_json")
	private String weekdaysJson;
	@Column(name = "start_date")
	private String startDate;
	@Column(name = "end_date")
	private String endDate;
	@Column(name = "start_time")
	private String startTime;
	@Column(name = "end_time")
	private String endTime;
//    @DynamoDBAttribute
//    private Map<String, Long> paymentOptions;
	@Column(name = "timezone")
	private String timezone;
}
