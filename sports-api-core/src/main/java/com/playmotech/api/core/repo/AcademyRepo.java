package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.Academy;

@Repository
public interface AcademyRepo extends JpaRepository<Academy, String> {

	List<Academy> findByIdInAndInactiveFalse(List<String> ids);

	List<Academy> findByManagerUserIdAndInactiveFalse(String managerUserId);

	// List<Academy> findByDomainUrl(String domainUrl);

	@Query("SELECT a FROM Academy a WHERE a.org.domainUrl = :domainUrl")
	List<Academy> findByDomainUrl(@Param("domainUrl") String domainUrl);

	Optional<Academy> findByIdAndInactiveFalse(String id);

	List<Academy> findByInactiveFalse();

	// @Query("SELECT DISTINCT a FROM Academy a JOIN CoachAcademyMapping c on
	// c.academy.id = a.id WHERE c.coachUserProfile.id = :coachUserId AND a.inactive
	// = false AND a.domainUrl = :domainUrl")
	// List<Academy> findByUserIdAndInactiveFalse(@Param("coachUserId") String
	// coachUserId,
	// @Param("domainUrl") String domainUrl);

	@Query("SELECT DISTINCT a FROM Academy a JOIN CoachAcademyMapping c ON c.academy.id = a.id WHERE c.coachUserProfile.id = :coachUserId AND a.inactive = false AND a.org.domainUrl = :domainUrl")
	List<Academy> findByUserIdAndInactiveFalse(@Param("coachUserId") String coachUserId,
			@Param("domainUrl") String domainUrl);

	List<Academy> findByInactiveFalseAndManagerUserId(String userId);

	@Query("SELECT a FROM Academy a WHERE a.org.id = :orgId")
	List<Academy> findByOrgId(@Param("orgId") String orgId);

	@Query("SELECT a FROM Academy a WHERE a.org.appPackageName = :appPackageName")
	List<Academy> findByAppPackageName(@Param("appPackageName") String appPackageName);
}
