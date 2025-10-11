package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.Academy_;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping_;
import com.playmotech.api.core.dao_postgres.Course_;
import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dao_postgres.TrialFeedback_;
import com.playmotech.api.core.dao_postgres.Trial_;
import com.playmotech.api.core.dao_postgres.UserProfile_;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class TrialSpecification implements Specification<Trial> {

	private final transient GenericFilter filter;

	private static boolean isValidOrderByColumn(String column) {
		return Trial_.NAME.equals(column) || Trial_.EMAIL.equals(column) || Trial_.STATUS.equals(column)
				|| Trial_.PHONE.equals(column) || Trial_.INSERTED_ON.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<Trial> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private void addOrderBy(Root<Trial> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(Trial_.INSERTED_ON))
						: builder.desc(root.get(Trial_.INSERTED_ON)));
			}
		}
	}

	private void addSearchLikePredicate(Root<Trial> root, CriteriaBuilder builder, List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";
			predicates.add(builder.or(builder.like(builder.lower(root.get(Trial_.NAME)), search),
					builder.like(builder.lower(root.get(Trial_.PHONE)), search),
					builder.like(builder.lower(root.get(Trial_.STATUS)), search),
					builder.like(builder.lower(root.get(Trial_.SPORTS)), search),
					builder.like(builder.lower(root.get(Trial_.EMAIL)), search)));
		}
	}

	private void addDateTimeRangePredicate(Root<Trial> root, CriteriaBuilder builder, List<Predicate> predicates) {
		if (filter.getStartDate() != null) {
			LocalDateTime start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.greaterThanOrEqualTo(root.get(Trial_.INSERTED_ON), start));
		}

		if (filter.getEndDate() != null) {
			LocalDateTime end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.lessThanOrEqualTo(root.get(Trial_.INSERTED_ON), end));
		}
	}

	private void addUserSpecificPredicates(Root<Trial> root, CriteriaBuilder builder, List<Predicate> predicates) {

		Subquery<String> subquery = builder.createQuery().subquery(String.class);
		Root<CoachAcademyMapping> subRoot = subquery.from(CoachAcademyMapping.class);

		Predicate academyMatch = builder.equal(subRoot.get(CoachAcademyMapping_.ACADEMY), root.get(Trial_.ACADEMY));

		Predicate userMatch = builder.equal(subRoot.get(CoachAcademyMapping_.COACH_USER_PROFILE).get(UserProfile_.ID),
				filter.getUserId());

		subquery.select(subRoot.get(CoachAcademyMapping_.ID)).where(academyMatch, userMatch);

		predicates.add(builder.exists(subquery));
	}

	private void addCoachSpecificPredicates(Root<Trial> root, CriteriaBuilder builder, List<Predicate> predicates) {

		predicates.add(builder.and(builder.isNotNull(root.join(Trial_.COACH).get(UserProfile_.ID)),
				builder.equal(root.join(Trial_.COACH).get(UserProfile_.ID), filter.getUserId())));

	}

	@Override
	public Predicate toPredicate(Root<Trial> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();

		if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addCoachSpecificPredicates(root, builder, predicates);
		} else if (!RoleType.SUPER_ADMIN.name().equalsIgnoreCase(userRole)) {
			addUserSpecificPredicates(root, builder, predicates);
		}

		// Search
		addSearchLikePredicate(root, builder, predicates);

		addTripleJoinInPredicate(root, builder, predicates, Trial_.FEEDBACK, TrialFeedback_.COURSE, Course_.ID,
				filter.getProgramIds());
		addInPredicate(root, builder, predicates, Trial_.SPORTS, filter.getSports());

		addJoinInPredicate(root, builder, predicates, Trial_.ACADEMY, Academy_.ID, filter.getAcademyId());
		addJoinInPredicate(root, builder, predicates, Trial_.ACADEMY, Academy_.ID, filter.getAcademyIds());
		addJoinInPredicate(root, builder, predicates, Trial_.FEEDBACK, TrialFeedback_.AGE_GROUP,
				filter.getAgeCategory());
		addEqualityPredicate(root, builder, predicates, Trial_.DELETED, !filter.isNotDeleted());
		addEqualityPredicate(root, builder, predicates, Trial_.STATUS, filter.getTrial().getStatus());
		addEqualityPredicate(root, builder, predicates, Trial_.COMPLETED, filter.getTrial().isCompleted());
//	    addAcaEqualityPredicate(root, builder, predicates, filter.getAcademyIds());

		// Date range
		addDateTimeRangePredicate(root, builder, predicates);

		// Order by
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addJoinInPredicate(Root<Trial> root, CriteriaBuilder builder, List<Predicate> predicates, String field,
			String id, String academyId) {
		if (academyId != null && !academyId.isEmpty()) {
			predicates.add(root.join(field).get(id).in(academyId));
		}
	}

	private void addJoinInPredicate(Root<Trial> root, CriteriaBuilder builder, List<Predicate> predicates, String field,
			String id, List<String> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.join(field).get(id).in(values));
		}

	}

	private void addTripleJoinInPredicate(Root<Trial> root, CriteriaBuilder builder, List<Predicate> predicates,
			String firstJoinField, String secondJoinField, String targetField, List<String> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.join(firstJoinField) // Join Trial -> TrialFeedback
					.join(secondJoinField) // Join TrialFeedback -> Course
					.get(targetField) // Course.id
					.in(values) // Filter based on values
			);
		}
	}

	// Helper method for IN predicates
	private static <T> void addInPredicate(Root<Trial> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

}
