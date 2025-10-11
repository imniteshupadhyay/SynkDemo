package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.PickleballTournament;

@Repository
public interface PickleballTournamentRepo
        extends JpaRepository<PickleballTournament, String>, JpaSpecificationExecutor<PickleballTournament> {
    List<PickleballTournament> findByAcademy_Id(String academyId);

    List<PickleballTournament> findByAcademyIsNull();

    Optional<PickleballTournament> findByIdAndAcademy_Id(String id, String academyId);

    Optional<PickleballTournament> findByIdAndAcademyIsNull(String id);

    List<PickleballTournament> findByAcademyIsNullAndInactive(boolean inactive);

    List<PickleballTournament> findByAcademy_IdAndInactive(String academyId, boolean inactive);

    @Query("SELECT t FROM PickleballTournament t WHERE t.name LIKE %:name%")
    List<PickleballTournament> findByNameContaining(String name);

    List<PickleballTournament> findByAcademy_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(String academyId,
            String startDate, String endDate);

    List<PickleballTournament> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(String startDate, String endDate);

    List<PickleballTournament> findByAcademy_IdAndStartDateGreaterThan(String academyId, String todayDate);

    List<PickleballTournament> findByStartDateGreaterThan(String startDate);

    List<PickleballTournament> findByAcademy_IdAndEndDateLessThan(String academyId, String todayDate);

    List<PickleballTournament> findByEndDateLessThan(String todayDate);

    Optional<PickleballTournament> findByAcademyIsNotNullAndId(String tournamentId);

    Optional<PickleballTournament> findByAcademyIsNullAndId(String tournamentId);
}
