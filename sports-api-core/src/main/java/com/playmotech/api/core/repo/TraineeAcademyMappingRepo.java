package com.playmotech.api.core.repo;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;

@Repository
public interface TraineeAcademyMappingRepo extends CrudRepository<TraineeAcademyMapping, String> {
	List<TraineeAcademyMapping> findByAcademy_Id(String academyId);

	Optional<TraineeAcademyMapping> findByAcademy_IdAndTraineeUserProfile_Id(String academyId, String traineeUserId);

	List<TraineeAcademyMapping> findByAcademy_IdAndTraineeUserProfile_IdIn(String academyId, List<String> ids);

	List<TraineeAcademyMapping> findByTraineeUserProfile_Id(String traineeUserId);

	List<TraineeAcademyMapping> findByAcademy_IdAndCreatedOnLessThanAndCreatedOnGreaterThanEqual(String academyId,
			Timestamp createdOn, Timestamp createdOn2);

	List<TraineeAcademyMapping> findByAcademy_IdAndCreatedOnGreaterThanEqual(String academyId, Timestamp createdOn);

	boolean existsByTraineeUserProfileIdAndAcademyId(String id, String academyId);

	List<TraineeAcademyMapping> findByTraineeUserProfileId(String id);

	List<TraineeAcademyMapping> findByAcademyIdIn(Set<String> academyIds);

}
