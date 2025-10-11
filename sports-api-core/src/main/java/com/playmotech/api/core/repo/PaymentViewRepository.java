package com.playmotech.api.core.repo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.playmotech.api.core.response.dao.PaymentTrendDao;
import com.playmotech.api.core.views.PaymentDetailsView;

public interface PaymentViewRepository
		extends JpaRepository<PaymentDetailsView, String>, JpaSpecificationExecutor<PaymentDetailsView> {

	@Query(value = "SELECT * FROM get_payment_trend(:startDate, :endDate, :userId, :academyIds, :sports, :courseIds, :ageCategories, :userRole, :domainUrl)", nativeQuery = true)
	List<Object[]> getPaymentTrendRaw(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate,
			@Param("userId") String userId, @Param("academyIds") String academyIds, @Param("sports") String sports,
			@Param("courseIds") String courseIds, @Param("ageCategories") String ageCategories,
			@Param("userRole") String userRole, @Param("domainUrl") String domainUrl);

	@Query(value = "SELECT * FROM get_course_enrollment_details( " + ":userId, " + ":academyIds, " + ":courseIds, "
			+ ":sports, " + ":ageCategories," + ":userRole, " + ":domainUrl ) ", nativeQuery = true)
	List<Object[]> findCourseEnrollmentDetails(@Param("userId") String userId, @Param("academyIds") String academyIds,
			@Param("courseIds") String courseIds, // Pass as comma-separated string,
			@Param("sports") String sports, @Param("ageCategories") String ageCategories,
			@Param("userRole") String userRole, @Param("domainUrl") String domainUrl);

	default List<PaymentTrendDao> getPaymentTrend(Timestamp startDate, Timestamp endDate, String userId,
			String academyIds, String sports, String courseIds, String ageCategories, String userRole,
			String domainUrl) {
		List<Object[]> results = getPaymentTrendRaw(startDate, endDate, userId, academyIds, sports, courseIds,
				ageCategories, userRole, domainUrl);
		return results.stream()
				.map(row -> new PaymentTrendDao((String) row[0], row[1] != null ? ((Number) row[1]).longValue() : null,
						row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO,
						row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO,
						row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO))
				.toList();
	}

	@Query(value = "SELECT * FROM payment_details_view p WHERE :userId = ANY(p.coach_ids)", nativeQuery = true)
	List<PaymentDetailsView> findByCoachId(@Param("userId") String userId);

}