package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.PerformanceReportStatus;
import com.playmotech.api.core.dao_postgres.TraineePerformanceReport;

@Repository
public interface TraineePerformanceRepo extends CrudRepository<TraineePerformanceReport, String> {
    List<TraineePerformanceReport> findByTraineeUserProfile_Id(String traineeUserId);

    List<TraineePerformanceReport> findByTraineeUserProfile_IdAndStatus(String traineeUserId,
                                                                        PerformanceReportStatus status);

    List<TraineePerformanceReport> findByTraineeUserProfile_IdAndStatusAndCourse_Id(String traineeUserId,
                                                                                    PerformanceReportStatus status, String courseId);

    List<TraineePerformanceReport> findByCoachUserProfile_Id(String coachUserId);

    List<TraineePerformanceReport> findByCoachUserProfile_IdAndStatus(String coachUserId,
                                                                      PerformanceReportStatus status);

    List<TraineePerformanceReport> findByCoachUserProfile_IdAndAcademy_Id(String coachUserId, String academyId);

    List<TraineePerformanceReport> findByCoachUserProfile_IdAndAcademy_IdAndStatus(String coachUserId, String academyId,
                                                                                   PerformanceReportStatus status);

    List<TraineePerformanceReport> findByCoachUserProfile_IdAndTraineeUserProfile_IdAndAcademy_Id(String coachUserId,
                                                                                                  String traineeUserId, String academyId);

    List<TraineePerformanceReport> findByCoachUserProfile_IdAndTraineeUserProfile_IdAndAcademy_IdAndStatus(
            String coachUserId, String traineeUserId, String academyId, PerformanceReportStatus status);

    List<TraineePerformanceReport> findByAcademy_Id(String academyId);

    List<TraineePerformanceReport> findByAcademy_IdAndStatus(String academyId, PerformanceReportStatus status);
}
