package com.playmotech.api.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.playmotech.api.core.response.dao.CoachPerformanceReportViewDao;
import com.playmotech.api.core.response.dao.TraineePerformanceReportViewDao;
import com.playmotech.api.core.views.CoachPerformanceReportView;
import com.playmotech.api.core.views.TraineePerformanceReportView;

@Component
public class PerformanceViewMapper {

	// Method to map a single TraineePerformanceReportView entity to
	// TraineePerformanceReportViewDao
	public static TraineePerformanceReportViewDao mapToDao(TraineePerformanceReportView reportView) {
		TraineePerformanceReportViewDao reportViewDao = new TraineePerformanceReportViewDao();
		try {
			BeanUtils.copyProperties(reportView, reportViewDao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reportViewDao;
	}

	public static List<TraineePerformanceReportViewDao> mapListToDaoList(
			List<TraineePerformanceReportView> reportViews) {
		return reportViews.stream().map(PerformanceViewMapper::mapToDao).collect(Collectors.toList());
	}

	public static CoachPerformanceReportViewDao mapToCoachDao(CoachPerformanceReportView reportView) {
		CoachPerformanceReportViewDao reportViewDao = new CoachPerformanceReportViewDao();
		try {
			BeanUtils.copyProperties(reportView, reportViewDao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reportViewDao;
	}

	public static List<CoachPerformanceReportViewDao> mapListToCoachDaoList(
			List<CoachPerformanceReportView> caochRecords) {
		return caochRecords.stream().map(PerformanceViewMapper::mapToCoachDao).collect(Collectors.toList());
	}

}
