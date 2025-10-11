package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.AttendanceView_;
import com.playmotech.api.core.views.TraineePerformanceReportView;
import com.playmotech.api.core.views.TraineePerformanceReportView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class TraineePerformanceSpecification implements Specification<TraineePerformanceReportView> {

	private final transient GenericFilter filter;

	private final transient UserProfileHelper userProfileHelper;

	/**
	 * Validates if the order by column exists in the view.
	 */
	private static boolean isValidOrderByColumn(String column) {
		return TraineePerformanceReportView_.PLAYER_NAME.equals(column)
				|| TraineePerformanceReportView_.COACH_NAME.equals(column)
				|| TraineePerformanceReportView_.REPORT_STATUS.equals(column)
				|| TraineePerformanceReportView_.REPORT_TITLE.equals(column)
				|| TraineePerformanceReportView_.ACADEMY.equals(column)
				|| TraineePerformanceReportView_.BRANCH.equals(column)
				|| TraineePerformanceReportView_.SPORT.equals(column)
				|| TraineePerformanceReportView_.PROGRAM.equals(column)
				|| TraineePerformanceReportView_.CREATED_ON.equals(column);
	}

	/**
	 * Adds equality predicates to the query.
	 */
	private static <T> void addEqualityPredicate(Root<TraineePerformanceReportView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	/**
	 * Adds sorting based on the selected order by column.
	 */
	private void addOrderBy(Root<TraineePerformanceReportView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				// Default order by created_on
				query.orderBy(filter.isAscending() ? builder.asc(root.get(TraineePerformanceReportView_.CREATED_ON))
						: builder.desc(root.get(TraineePerformanceReportView_.CREATED_ON)));
			}
		}
	}

	private void ownerSpecificStations(Root<TraineePerformanceReportView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		predicates.add(builder.equal(root.get(AttendanceView_.MANAGER_ID), filter.getUserId()));
	}

	/**
	 * Adds search filter across multiple fields.
	 */
	private void addSearchLikePredicate(Root<TraineePerformanceReportView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			// Search across multiple fields
			predicates.add(
					builder.or(builder.like(builder.lower(root.get(TraineePerformanceReportView_.PLAYER_NAME)), search),
							builder.like(builder.lower(root.get(TraineePerformanceReportView_.COACH_NAME)), search),
							builder.like(builder.lower(root.get(TraineePerformanceReportView_.REPORT_TITLE)), search),
							builder.like(builder.lower(root.get(TraineePerformanceReportView_.REPORT_STATUS)), search),
							builder.like(builder.lower(root.get(TraineePerformanceReportView_.ACADEMY)), search),
							builder.like(builder.lower(root.get(TraineePerformanceReportView_.BRANCH)), search),
							builder.like(builder.lower(root.get(TraineePerformanceReportView_.ENROLL_ID)), search),
							builder.like(builder.lower(root.get(TraineePerformanceReportView_.SPORT)), search),
							builder.like(builder.lower(root.get(TraineePerformanceReportView_.PROGRAM)), search)));
		}
	}

	/**
	 * Adds date filtering based on the `created_on` column.
	 */
//	private void addCreatedOnDateFilter(Root<TraineePerformanceReportView> root, CriteriaBuilder builder,
//			List<Predicate> predicates) {
//		if (filter.getDays() != null && filter.getDays() > 0) {
//			LocalDate fromDate = LocalDate.now().minusDays(filter.getDays());
//			predicates.add(builder.greaterThanOrEqualTo(root.get(TraineePerformanceReportView_.CREATED_ON), fromDate));
//		}
//	}

	private void addDateTimeRangePredicate(Root<TraineePerformanceReportView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
// Convert java.util.Date or Timestamp to LocalDateTime
		if (filter.getStartDate() != null) {
			LocalDateTime start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.greaterThanOrEqualTo(root.get(TraineePerformanceReportView_.CREATED_ON), start));
		}

		if (filter.getEndDate() != null) {
			LocalDateTime end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.lessThanOrEqualTo(root.get(TraineePerformanceReportView_.CREATED_ON), end));
		}
	}

	private static <T> void addInPredicate(Root<TraineePerformanceReportView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private void addInPredicateIgnoreCase(Root<TraineePerformanceReportView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<String> values) {
		if (values != null && !values.isEmpty()) {
			List<Predicate> inPredicates = new ArrayList<>();
			for (String value : values) {
				inPredicates.add(builder.equal(builder.lower(root.get(field)), value.toLowerCase()));
			}
			predicates.add(builder.or(inPredicates.toArray(new Predicate[0])));
		}
	}

	@Override
	public Predicate toPredicate(Root<TraineePerformanceReportView> root, CriteriaQuery<?> query,
			CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();

		String domainUrl = filter.getDomainUrl();

		// Add search like predicate
		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			ownerSpecificStations(root, builder, predicates);
		}
		if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, TraineePerformanceReportView_.COACH_IDS,
					TraineePerformanceReportView_.DOMAIN_URL, filter.getUserId(), domainUrl);

		}
		if (RoleType.ADMIN.getRole().equalsIgnoreCase(userRole)
				|| RoleType.CLUSTER_HEAD.getRole().equalsIgnoreCase(userRole)
				|| RoleType.PROGRAM_MANAGER.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, TraineePerformanceReportView_.MAINTAINER_IDS,
					TraineePerformanceReportView_.DOMAIN_URL, filter.getUserId(), domainUrl);
		}

		// Add search filter
		addSearchLikePredicate(root, builder, predicates);

		// Add equality filters
//		addEqualityPredicate(root, builder, predicates, TraineePerformanceReportView_.ACADEMY_ID,
//				filter.getAcademyId());
		addEqualityPredicate(root, builder, predicates, TraineePerformanceReportView_.BRANCH_ID, filter.getBranchId());
		addEqualityPredicate(root, builder, predicates, TraineePerformanceReportView_.PROGRAM_ID,
				filter.getProgramId());
		addEqualityPredicate(root, builder, predicates, TraineePerformanceReportView_.SPORT, filter.getSport());

		// Add "IN" filters for branchIds and programIds
		addInPredicate(root, builder, predicates, TraineePerformanceReportView_.BRANCH_ID, filter.getBranchIds());
		addInPredicate(root, builder, predicates, TraineePerformanceReportView_.ACADEMY_ID, filter.getAcademyIds());
		addInPredicate(root, builder, predicates, TraineePerformanceReportView_.PROGRAM_ID, filter.getProgramIds());

		addInPredicate(root, builder, predicates, TraineePerformanceReportView_.PLAYER_ID, filter.getPlayerIds());

		addInPredicate(root, builder, predicates, TraineePerformanceReportView_.COACH_ID, filter.getCoachIds());

		addInPredicateIgnoreCase(root, builder, predicates, TraineePerformanceReportView_.SPORT, filter.getSports());
		addInPredicateIgnoreCase(root, builder, predicates, TraineePerformanceReportView_.AGE_CATEGORY,
				filter.getAgeCategory());
		// time range filter
		addDateTimeRangePredicate(root, builder, predicates);
		// Add sorting
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addInPredicate(Root<TraineePerformanceReportView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, String userId) {
		predicates.add(
				builder.like(builder.function("array_to_string", String.class, root.get(field), builder.literal(",")),
						"%" + userId + "%"));
	}

	private void addInPredicate(Root<TraineePerformanceReportView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, String nextFeild, String userId, String domainUrl) {

		// Combine both conditions into a single AND predicate
		Predicate combinedPredicate = builder.and(
				builder.like(builder.function("array_to_string", String.class, root.get(field), builder.literal(",")),
						"%" + userId + "%"),
				builder.equal(root.get(nextFeild), domainUrl) // Check if domainUrl matches
		);

		// Add the combined predicate to the list
		predicates.add(combinedPredicate);
	}
}
