package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.playmotech.api.core.constants.DiscoverCategory;
import com.playmotech.api.core.dao_postgres.Discover;

public interface DiscoverRepo extends JpaRepository<Discover, String> {
//    List<Discover> findByTypeIgnoreCase(String type);
//
//    List<Discover> findByLocationIgnoreCase(String location);
//
//    List<Discover> findByTypeIgnoreCaseAndLocationIgnoreCase(String type, String location);

    List<Discover> findByType(DiscoverCategory category);

    List<Discover> findByAffiliatedTrue();

    List<Discover> findByScrapedTrue();

    @Query(value = """
    	    SELECT * FROM discover d
    	    WHERE ST_DWithin(
    	        ST_MakePoint(d.longitude, d.latitude),
    	        ST_MakePoint(:longitude, :latitude),
    	        :radiusKm * 1000
    	    )
    	    AND (:type IS NULL OR d.type = CAST(:type AS text))
    	    ORDER BY ST_Distance(
    	        ST_MakePoint(d.longitude, d.latitude),
    	        ST_MakePoint(:longitude, :latitude)
    	    )
    	    """, nativeQuery = true)
    	List<Discover> findByProximity(
    	    @Param("latitude") Double latitude,
    	    @Param("longitude") Double longitude,
    	    @Param("radiusKm") Integer radiusKm,
    	    @Param("type") String type
    	);
    
//    @Query(value = 
//        "SELECT * FROM discover d " +
//        "WHERE ST_DWithin(" +
//        "    ST_MakePoint(d.longitude, d.latitude)::geography, " +
//        "    ST_MakePoint(?2, ?1)::geography, " +
//        "    ?3 * 1000" +  // Convert km to meters
//        ") " +
//        "AND (?4 IS NULL OR d.type = ?4::text) " +
//        "ORDER BY ST_Distance(" +
//        "    ST_MakePoint(d.longitude, d.latitude)::geography, " +
//        "    ST_MakePoint(?2, ?1)::geography" +
//        ")",
//        nativeQuery = true)
//    List<Discover> findByProximity(Double latitude, Double longitude, Integer radiusKm, String type);

    // Advanced filtering with JPA Specification
    @Query("SELECT d FROM Discover d " +
           "WHERE (?1 IS NULL OR d.type = ?1) " +
           "AND (?2 IS NULL OR d.rating >= ?2) " +
           "AND (?3 IS NULL OR d.active = ?3) " +
           "AND (?4 IS NULL OR d.featured = ?4)")
    List<Discover> findWithFilters(DiscoverCategory type, Double minRating, Boolean active, Boolean featured);

    // Find by tags (any match)
    @Query("SELECT DISTINCT d FROM Discover d JOIN d.tags t " +
           "WHERE t IN :tags")
    List<Discover> findByTagsIn(List<String> tags);

}
