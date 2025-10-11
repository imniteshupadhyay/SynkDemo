package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.GeoFence;

@Repository
public interface GeoFenceRepository extends JpaRepository<GeoFence, Long> {

	List<GeoFence> findByAcademy_IdAndDeletedFalse(String academyId);

	Optional<GeoFence> findByAcademy_IdAndGeoFenceIdAndDeletedFalse(String academyId, Long geoFenceId);

	Optional<GeoFence> findByAcademy_IdAndNameIgnoreCaseAndDeletedFalse(String academyId, String name);

	List<GeoFence> findByAcademy_IdAndUserTypeInAndDeletedFalse(String academyId, List<String> userTypes);

	List<GeoFence> findByAcademy_IdInAndUserTypeInAndDeletedFalse(List<String> academyIds, List<String> userTypes);
}
