package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.UsersActionsMapping;

public interface UsersActionsMappingRepo extends JpaRepository<UsersActionsMapping, Long> {
	Optional<UsersActionsMapping> findByUser(UserProfile user);

	@Override
	@Modifying
	@Query(value = "DELETE FROM user_role_action_mapping uram WHERE uram.id = :id", nativeQuery = true)
	void deleteById(@Param("id") Long id);

	@Query("SELECT uam FROM UsersActionsMapping uam WHERE uam.user.id IN :userIds")
	List<UsersActionsMapping> findByUserIdIn(@Param("userIds") List<String> userIds);

}
