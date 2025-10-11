package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.BadmintonTournament;

@Repository
public interface BadmintonTournamentRepo
		extends CrudRepository<BadmintonTournament, String>, JpaSpecificationExecutor<BadmintonTournament> {
	List<BadmintonTournament> findByAcademy_Id(String academyId);

	List<BadmintonTournament> findByAcademy_IdAndStartDateGreaterThan(String academyId, String startDate);

	List<BadmintonTournament> findByStartDateGreaterThan(String startDate);

	List<BadmintonTournament> findByAcademy_IdAndEndDateLessThan(String academyId, String endDate);

	List<BadmintonTournament> findByEndDateLessThan(String endDate);

	List<BadmintonTournament> findByAcademy_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(String academyId,
			String startDate, String endDate);

	List<BadmintonTournament> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(String startDate, String endDate);

	Optional<BadmintonTournament> findByAcademyIsNullAndId(String id);

	Optional<BadmintonTournament> findByAcademyIsNotNullAndId(String id);
}
