package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

//import com.playmotech.api.core.dao.AcademyLead;
import com.playmotech.api.core.dao_postgres.Court;

@Repository
@EnableScan
public interface CourtRepo extends CrudRepository<Court, String> {
	List<Court> findByAcademy_Id(String academyId);

	@Query("SELECT u FROM Court u WHERE u.courtName ILIKE %:searchTxt% AND academy.id = :academyId")
	List<Court> findByAcademy_IdAndSearchByCourtName(@Param("searchTxt") String searchTxt,
			@Param("academyId") String academyId);

	@Query("SELECT u FROM Court u WHERE u.courtName ILIKE %:searchTxt% AND academy.id is null")
	List<Court> findByAcademyIsNull(@Param("searchTxt") String searchTxt);

	List<Court> findByAcademyIsNull();

	Optional<Court> findByAcademyIsNullAndId(String id);
}
