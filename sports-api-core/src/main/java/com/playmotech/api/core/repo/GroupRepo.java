package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

//import com.playmotech.api.core.dao.AcademyLead;
import com.playmotech.api.core.dao_postgres.Group;

@Repository
@EnableScan
public interface GroupRepo extends CrudRepository<Group, String> {
	Optional<Group> findByAcademy_IdAndId(String academyId, String id);

	List<Group> findByAcademy_Id(String academyId);

	List<Group> findByInactive(Boolean Inactive);
}
