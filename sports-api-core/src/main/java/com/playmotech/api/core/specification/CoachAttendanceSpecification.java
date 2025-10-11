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
import com.playmotech.api.core.views.CoachAttendanceView;
import com.playmotech.api.core.views.CoachAttendanceView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class CoachAttendanceSpecification implements Specification<CoachAttendanceView> {

	private final transient GenericFilter filter;
	private final transient UserProfileHelper userProfileHelper;

	private static boolean isValidOrderByColumn(String column) {
		return CoachAttendanceView_.coachName.getName().equals(column)
				|| CoachAttendanceView_.academy.getName().equals(column)
				|| CoachAttendanceView_.sport.getName().equals(column)
				|| CoachAttendanceView_.status.getName().equals(column)
				|| CoachAttendanceView_.attendanceDate.getName().equals(column)
				|| CoachAttendanceView_.program.getName().equals(column)
				|| CoachAttendanceView_.recordCreatedAt.getName().equals(column);
	}

	private static <T> void addEqualityPredicate(Root<CoachAttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates, SingularAttribute<CoachAttendanceView, T> field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private void addOrderBy(Root<CoachAttendanceView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(CoachAttendanceView_.recordCreatedAt))
						: builder.desc(root.get(CoachAttendanceView_.recordCreatedAt)));
			}
		}
	}

	private void addSearchLikePredicate(Root<CoachAttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			predicates.add(builder.or(builder.like(builder.lower(root.get(CoachAttendanceView_.coachName)), search),
					builder.like(builder.lower(root.get(CoachAttendanceView_.academy)), search),
					builder.like(builder.lower(root.get(CoachAttendanceView_.sport)), search),
					builder.like(builder.lower(root.get(CoachAttendanceView_.program)), search),
					builder.like(builder.lower(root.get(CoachAttendanceView_.status)), search)));
		}
	}

	private void ownerSpecificStations(Root<CoachAttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		predicates.add(builder.equal(root.get(CoachAttendanceView_.managerId), filter.getUserId()));
	}

	private void addDateTimeRangePredicate(Root<CoachAttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (filter.getStartDate() != null) {
			LocalDateTime start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(
					builder.greaterThanOrEqualTo(root.get(CoachAttendanceView_.attendanceDate), start.toLocalDate()));
		}

		if (filter.getEndDate() != null) {
			LocalDateTime end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.lessThanOrEqualTo(root.get(CoachAttendanceView_.attendanceDate), end.toLocalDate()));
		}
	}

	private static <T> void addInPredicate(Root<CoachAttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates, SingularAttribute<CoachAttendanceView, T> field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private static void addInPredicateIgnoreCase(Root<CoachAttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates, SingularAttribute<CoachAttendanceView, String> field, List<String> values) {
		if (values != null && !values.isEmpty()) {
			List<Predicate> inPredicates = new ArrayList<>();
			for (String value : values) {
				inPredicates.add(builder.equal(builder.lower(root.get(field)), value.toLowerCase()));
			}
			predicates.add(builder.or(inPredicates.toArray(new Predicate[0])));
		}
	}

	@Override
	public Predicate toPredicate(Root<CoachAttendanceView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();
		String domainUrl = filter.getDomainUrl();

		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			ownerSpecificStations(root, builder, predicates);
		}

		if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addInPredicateForArray(root, builder, predicates, CoachAttendanceView_.coachIds, filter.getUserId(),
					domainUrl);
		}

		if (RoleType.ADMIN.getRole().equalsIgnoreCase(userRole)
				|| RoleType.CLUSTER_HEAD.getRole().equalsIgnoreCase(userRole)
				|| RoleType.PROGRAM_MANAGER.getRole().equalsIgnoreCase(userRole)) {
			addInPredicateForArray(root, builder, predicates, CoachAttendanceView_.maintainerIds, filter.getUserId(),
					domainUrl);
		}

		// Search filter
		addSearchLikePredicate(root, builder, predicates);

		// Equality filters
		addEqualityPredicate(root, builder, predicates, CoachAttendanceView_.programId, filter.getProgramId());
		addEqualityPredicate(root, builder, predicates, CoachAttendanceView_.sport, filter.getSport());
		addEqualityPredicate(root, builder, predicates, CoachAttendanceView_.coachId, filter.getCoachId());

		// IN filters
		addInPredicate(root, builder, predicates, CoachAttendanceView_.programId, filter.getProgramIds());
		addInPredicate(root, builder, predicates, CoachAttendanceView_.academyId, filter.getAcademyIds());

		// Ignore-case filters
		addInPredicateIgnoreCase(root, builder, predicates, CoachAttendanceView_.ageCategory, filter.getAgeCategory());
		addInPredicateIgnoreCase(root, builder, predicates, CoachAttendanceView_.sport, filter.getSports());

		// Date range
		addDateTimeRangePredicate(root, builder, predicates);

		// Sorting
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addInPredicateForArray(Root<CoachAttendanceView> root, CriteriaBuilder builder,
			List<Predicate> predicates, SingularAttribute<CoachAttendanceView, List<String>> field, String userId,
			String domainUrl) {

		Predicate combined = builder.and(
				builder.like(builder.function("array_to_string", String.class, root.get(field), builder.literal(",")),
						"%" + userId + "%"),
				builder.equal(root.get(CoachAttendanceView_.domainUrl), domainUrl));

		predicates.add(combined);
	}
}
