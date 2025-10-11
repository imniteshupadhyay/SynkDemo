package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.Academy_;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory_;
import com.playmotech.api.core.dao_postgres.Course_;
import com.playmotech.api.core.dao_postgres.UserProfile_;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class BulkUploadHistorySpecification implements Specification<BulkUploadHistory> {

	private final transient GenericFilter filter;

	private static boolean isValidOrderByColumn(String column) {
		return BulkUploadHistory_.TYPE.equals(column) || BulkUploadHistory_.STATUS.equals(column)
				|| BulkUploadHistory_.FILE_NAME.equals(column) || BulkUploadHistory_.TOTAL_RECORDS.equals(column)
				|| BulkUploadHistory_.SUCCESS_COUNT.equals(column) || BulkUploadHistory_.FAILURE_COUNT.equals(column)
				|| BulkUploadHistory_.STARTED_AT.equals(column) || BulkUploadHistory_.COMPLETED_AT.equals(column)
				|| BulkUploadHistory_.INSERTED_ON.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<BulkUploadHistory> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private static void addAcademyEqualityPredicate(Root<BulkUploadHistory> root, CriteriaBuilder builder,
			List<Predicate> predicates, String academyId) {
		if (academyId != null) {
			predicates.add(builder.equal(root.join(BulkUploadHistory_.ACADEMY).get(Academy_.ID), academyId));
		}
	}

	private static void addProgramEqualityPredicate(Root<BulkUploadHistory> root, CriteriaBuilder builder,
			List<Predicate> predicates, String programId) {
		if (programId != null) {
			predicates.add(builder.equal(root.join(BulkUploadHistory_.PROGRAM).get(Course_.ID), programId));
		}
	}

	private static void addUserEqualityPredicate(Root<BulkUploadHistory> root, CriteriaBuilder builder,
			List<Predicate> predicates, String userId) {
		if (userId != null) {
			predicates.add(builder.equal(root.join(BulkUploadHistory_.UPLOADED_BY).get(UserProfile_.ID), userId));
		}
	}

	private void addOrderBy(Root<BulkUploadHistory> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(BulkUploadHistory_.INSERTED_ON))
						: builder.desc(root.get(BulkUploadHistory_.INSERTED_ON)));
			}
		}
	}

	private void addSearchLikePredicate(Root<BulkUploadHistory> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";
			predicates.add(builder.or(builder.like(builder.lower(root.get(BulkUploadHistory_.FILE_NAME)), search),
					builder.like(builder.lower(root.get(BulkUploadHistory_.REMARKS)), search)));
		}
	}

	private void addDateTimeRangePredicate(Root<BulkUploadHistory> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (filter.getStartDate() != null) {
			LocalDateTime start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.greaterThanOrEqualTo(root.get(BulkUploadHistory_.INSERTED_ON), start));
		}

		if (filter.getEndDate() != null) {
			LocalDateTime end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.lessThanOrEqualTo(root.get(BulkUploadHistory_.INSERTED_ON), end));
		}
	}

	private void addJoinInPredicate(Root<BulkUploadHistory> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String id, List<String> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.join(field).get(id).in(values));
		}
	}

	// Helper method for IN predicates
	private static <T> void addInPredicate(Root<BulkUploadHistory> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private void ownerSpecificCourses(Root<BulkUploadHistory> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		predicates.add(
				builder.equal(root.join(BulkUploadHistory_.ACADEMY).get(Academy_.MANAGER_USER_ID), filter.getUserId()));
	}

	@Override
	public Predicate toPredicate(Root<BulkUploadHistory> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();

		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			ownerSpecificCourses(root, builder, predicates);
		} else if (!RoleType.SUPER_ADMIN.getRole().equalsIgnoreCase(userRole)) {
			// User filter
			addUserEqualityPredicate(root, builder, predicates, filter.getUserId());
		}

		// Deleted filter
		addEqualityPredicate(root, builder, predicates, BulkUploadHistory_.DELETED, !filter.isNotDeleted());

		// Academy filter
		addAcademyEqualityPredicate(root, builder, predicates, filter.getAcademyId());

		// Program filter
		addProgramEqualityPredicate(root, builder, predicates, filter.getProgramId());

		// Academy IDs filter (for multiple academies)
		addJoinInPredicate(root, builder, predicates, BulkUploadHistory_.ACADEMY, Academy_.ID, filter.getAcademyIds());

		// Academy IDs filter (for multiple academies)
		addJoinInPredicate(root, builder, predicates, BulkUploadHistory_.PROGRAM, Course_.ID, filter.getProgramIds());

		// Type filter (if you have bulk types to filter)
		addInPredicate(root, builder, predicates, BulkUploadHistory_.TYPE, filter.getUploadHistory().getBulkTypes());

		// Status filter (if you have upload statuses to filter)
		addInPredicate(root, builder, predicates, BulkUploadHistory_.STATUS,
				filter.getUploadHistory().getUploadStatuses());

		// Search filter
		addSearchLikePredicate(root, builder, predicates);

		// Date range filter
		addDateTimeRangePredicate(root, builder, predicates);

		// Order by
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}
}