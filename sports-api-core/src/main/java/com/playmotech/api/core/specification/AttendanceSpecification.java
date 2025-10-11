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
import com.playmotech.api.core.views.AttendanceView;
import com.playmotech.api.core.views.AttendanceView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class AttendanceSpecification implements Specification<AttendanceView> {

	private final transient GenericFilter filter;

	private final transient UserProfileHelper userProfileHelper;

	private static boolean isValidOrderByColumn(String column) {
		return AttendanceView_.PLAYER_NAME.equals(column) || AttendanceView_.ACADEMY.equals(column)
				|| AttendanceView_.BRANCH.equals(column) || AttendanceView_.SPORT.equals(column)
				|| AttendanceView_.STATUS.equals(column) || AttendanceView_.NORMALIZED_DATE.equals(column)
				|| AttendanceView_.MARKED_BY.equals(column) || AttendanceView_.PROGRAM.equals(column)
				|| AttendanceView_.MARKED_AT.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<AttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private void addOrderBy(Root<AttendanceView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(AttendanceView_.MARKED_AT))
						: builder.desc(root.get(AttendanceView_.MARKED_AT)));
			}
		}
	}

	private void addSearchLikePredicate(Root<AttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			// Search across multiple fields
			predicates.add(builder.or(builder.like(builder.lower(root.get(AttendanceView_.PLAYER_NAME)), search),
					builder.like(builder.lower(root.get(AttendanceView_.ACADEMY)), search),
					builder.like(builder.lower(root.get(AttendanceView_.BRANCH)), search),
					builder.like(builder.lower(root.get(AttendanceView_.ENROLL_ID)), search),
					builder.like(builder.lower(root.get(AttendanceView_.SPORT)), search),
					builder.like(builder.lower(root.get(AttendanceView_.PROGRAM)), search)));
		}
	}

	private void ownerSpecificStations(Root<AttendanceView> root, CriteriaBuilder builder, List<Predicate> predicates) {
		predicates.add(builder.equal(root.get(AttendanceView_.MANAGER_ID), filter.getUserId()));
	}

	private void addDateTimeRangePredicate(Root<AttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
// Convert java.util.Date or Timestamp to LocalDateTime
		if (filter.getStartDate() != null) {
			LocalDateTime start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//			predicates.add(builder.greaterThanOrEqualTo(root.get(AttendanceView_.MARKED_AT), start));
			predicates.add(builder.greaterThanOrEqualTo(root.get(AttendanceView_.NORMALIZED_DATE), start));
		}

		if (filter.getEndDate() != null) {
			LocalDateTime end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//			predicates.add(builder.lessThanOrEqualTo(root.get(AttendanceView_.MARKED_AT), end));
			predicates.add(builder.lessThanOrEqualTo(root.get(AttendanceView_.NORMALIZED_DATE), end));
		}
	}

	private static <T> void addInPredicate(Root<AttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private static void addInPredicateIgnoreCase(Root<AttendanceView> root, CriteriaBuilder builder,
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
	public Predicate toPredicate(Root<AttendanceView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();

		String domainUrl = filter.getDomainUrl();
		// Add search like predicate
		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			ownerSpecificStations(root, builder, predicates);
		}
		if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, AttendanceView_.COACH_IDS, AttendanceView_.DOMAIN_URL,
					filter.getUserId(), domainUrl);

		}
		if (RoleType.ADMIN.getRole().equalsIgnoreCase(userRole)
				|| RoleType.CLUSTER_HEAD.getRole().equalsIgnoreCase(userRole)
				|| RoleType.PROGRAM_MANAGER.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, AttendanceView_.MAINTAINER_IDS, AttendanceView_.DOMAIN_URL,
					filter.getUserId(), domainUrl);
		}

		// Add search filter for all fields
		addSearchLikePredicate(root, builder, predicates);

		// Add equality filters
		addEqualityPredicate(root, builder, predicates, AttendanceView_.BRANCH_ID, filter.getBranchId());
		addEqualityPredicate(root, builder, predicates, AttendanceView_.PROGRAM_ID, filter.getProgramId());
		addEqualityPredicate(root, builder, predicates, AttendanceView_.SPORT, filter.getSport());

		addEqualityPredicate(root, builder, predicates, AttendanceView_.MARKED_BY_ID, filter.getCoachId());

		// Add "IN" filters for branchIds and programIds
		addInPredicate(root, builder, predicates, AttendanceView_.PROGRAM_ID, filter.getProgramIds());
		addInPredicate(root, builder, predicates, AttendanceView_.BRANCH_ID, filter.getBranchIds());
		addInPredicate(root, builder, predicates, AttendanceView_.ACADEMY_ID, filter.getAcademyIds());

		addInPredicate(root, builder, predicates, AttendanceView_.PLAYER_ID, filter.getPlayerIds());

		if (filter.getCoachId() != null && !filter.getCoachId().isEmpty()) {
			addInPredicate(root, builder, predicates, AttendanceView_.COACH_IDS, filter.getCoachId());
		}

		addInPredicateIgnoreCase(root, builder, predicates, AttendanceView_.AGE_CATEGORY, filter.getAgeCategory());
		addInPredicateIgnoreCase(root, builder, predicates, AttendanceView_.SPORT, filter.getSports());

		// time range filter
		addDateTimeRangePredicate(root, builder, predicates);

		// Add sorting
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addInPredicate(Root<AttendanceView> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String userId) {
		predicates.add(
				builder.like(builder.function("array_to_string", String.class, root.get(field), builder.literal(",")),
						"%" + userId + "%"));
	}

	private void addInPredicate(Root<AttendanceView> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String nextFeild, String userId, String domainUrl) {

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
