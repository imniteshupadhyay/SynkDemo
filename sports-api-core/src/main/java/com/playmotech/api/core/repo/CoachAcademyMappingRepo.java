package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;

@Repository
public interface CoachAcademyMappingRepo extends JpaRepository<CoachAcademyMapping, String> {

	List<CoachAcademyMapping> findByAcademy_Id(String academyId);

	List<CoachAcademyMapping> findByCoachUserProfile_Id(String coachUserId);

	Optional<CoachAcademyMapping> findByAcademy_IdAndCoachUserProfile_Id(String academyId, String coachUserId);

	List<CoachAcademyMapping> findByAcademy_IdAndCoachUserProfile_IdIn(String academyId, List<String> ids);

	Optional<CoachAcademyMapping> findByCoachUserProfileAndAcademy(UserProfile savedUser, Academy academy);

	List<CoachAcademyMapping> findByCoachUserProfile(UserProfile savedUser);

	List<CoachAcademyMapping> findByAcademy_IdInAndCoachUserProfile_Id(List<String> academyId, String coachUserId);

	List<CoachAcademyMapping> findByCoachUserProfileId(String id);

	boolean existsByCoachUserProfileIdAndAcademyId(String id, String academyId);

	List<CoachAcademyMapping> findByCoachUserProfile_IdAndAcademy_IdAndStatus(String coachId, String academyId,
			Status active);

	List<CoachAcademyMapping> findByCoachUserProfile_IdAndStatus(String coachId, Status active);
}
