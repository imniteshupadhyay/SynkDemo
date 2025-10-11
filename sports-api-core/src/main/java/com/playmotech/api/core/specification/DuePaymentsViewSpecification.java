package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.DuePaymentsView;
import com.playmotech.api.core.views.DuePaymentsView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class DuePaymentsViewSpecification implements Specification<DuePaymentsView> {

	private final transient GenericFilter filter;

	private final transient UserProfileHelper userProfileHelper;

	private static boolean isValidOrderByColumn(String column) {
		return DuePaymentsView_.PLAYER_NAME.equals(column) || DuePaymentsView_.ACADEMY_NAME.equals(column)
				|| DuePaymentsView_.SPORT.equals(column) || DuePaymentsView_.COURSE_TOTAL_PENDING.equals(column)
				|| DuePaymentsView_.PAYMENT_SCHEDULE.equals(column) || DuePaymentsView_.ENROLLMENT_ID.equals(column)
				|| DuePaymentsView_.COURSE_NAME.equals(column) || DuePaymentsView_.PLAYER_EMAIL_ID.equals(column)
				|| DuePaymentsView_.PLAYER_CONTACT_NUMBER.equals(column)
				|| DuePaymentsView_.REGISTRATION_DUE_DAYS_COUNT.equals(column)
				|| DuePaymentsView_.COURSE_DUE_DAYS_COUNT.equals(column)
				|| DuePaymentsView_.TOTAL_COURSE_DUE.equals(column)
				|| DuePaymentsView_.CURRENT_COURSE_DUE.equals(column)
				|| DuePaymentsView_.COURSE_TOTAL_PENDING.equals(column)
				|| DuePaymentsView_.COURSE_TOTAL_PAID.equals(column)
				|| DuePaymentsView_.COURSE_DISCOUNT_TOTAL.equals(column)
				|| DuePaymentsView_.COURSE_DUE_DAYS_STATUS.equals(column)
				|| DuePaymentsView_.REGISTRATION_DUE_DAYS_STATUS.equals(column)
				|| DuePaymentsView_.COURSE_DUE_DAYS_COUNT.equals(column)
				|| DuePaymentsView_.REGISTRATION_DUE_DAYS_COUNT.equals(column)
				|| DuePaymentsView_.REGISTRATION_TOTAL_PAID.equals(column)
				|| DuePaymentsView_.REGISTRATION_DISCOUNT_TOTAL.equals(column)
				|| DuePaymentsView_.REGISTRATION_TOTAL_PENDING.equals(column)
				|| DuePaymentsView_.FINAL_DUE_AMOUNT.equals(column) || DuePaymentsView_.DUES_ON.equals(column)
				|| DuePaymentsView_.JOINING_DATE.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<DuePaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private void addOrderBy(Root<DuePaymentsView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(DuePaymentsView_.PLAYER_NAME))
						: builder.desc(root.get(DuePaymentsView_.PLAYER_NAME)));
			}
		}
	}

	private void addSearchLikePredicate(Root<DuePaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			// Search across multiple fields
			predicates.add(builder.or(builder.like(builder.lower(root.get(DuePaymentsView_.PLAYER_NAME)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.ACADEMY_NAME)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.SPORT)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.ENROLLMENT_ID)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.REGISTRATION_TOTAL_PENDING).as(String.class)),
							search),
					builder.like(builder.lower(root.get(DuePaymentsView_.COURSE_TOTAL_PENDING).as(String.class)),
							search),
					builder.like(builder.lower(root.get(DuePaymentsView_.FINAL_DUE_AMOUNT).as(String.class)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.PLAYER_EMAIL_ID)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.PLAYER_CONTACT_NUMBER)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.COURSE_NAME)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.AGE_CATEGORY)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.PAYMENT_STATUS)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.REGISTRATION_DUE_DAYS_STATUS)), search),
					builder.like(builder.lower(root.get(DuePaymentsView_.COURSE_DUE_DAYS_STATUS)), search)));
		}
	}

	private void ownerSpecificStations(Root<DuePaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		predicates.add(builder.equal(root.get(DuePaymentsView_.MANAGER_USER_ID), filter.getUserId()));
	}

	private static <T> void addInPredicate(Root<DuePaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private static void addInPredicateIgnoreCase(Root<DuePaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<String> values) {
		if (values != null && !values.isEmpty()) {
			List<Predicate> inPredicates = new ArrayList<>();
			for (String value : values) {
				inPredicates.add(builder.equal(builder.lower(root.get(field)), value.toLowerCase(Locale.ENGLISH)));
			}
			predicates.add(builder.or(inPredicates.toArray(new Predicate[0])));
		}
	}

	private void addArrayContainsPredicate(Root<DuePaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, String userId) {
		predicates.add(
				builder.like(builder.function("array_to_string", String.class, root.get(field), builder.literal(",")),
						"%" + userId + "%"));
	}

	private void addArrayContainsWithDomainPredicate(Root<DuePaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String arrayField, String domainField, String userId, String domainUrl) {
		Predicate combinedPredicate = builder.and(builder.like(
				builder.function("array_to_string", String.class, root.get(arrayField), builder.literal(",")),
				"%" + userId + "%"), builder.equal(root.get(domainField), domainUrl));
		predicates.add(combinedPredicate);
	}

	@Override
	public Predicate toPredicate(Root<DuePaymentsView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();
		String domainUrl = filter.getDomainUrl();

		// Role-based filtering
		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			ownerSpecificStations(root, builder, predicates);
		}

		if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addArrayContainsWithDomainPredicate(root, builder, predicates, DuePaymentsView_.COACH_IDS,
					DuePaymentsView_.DOMAIN_URL, filter.getUserId(), domainUrl);
		}

		if (RoleType.ADMIN.getRole().equalsIgnoreCase(userRole)
				|| RoleType.CLUSTER_HEAD.getRole().equalsIgnoreCase(userRole)
				|| RoleType.PROGRAM_MANAGER.getRole().equalsIgnoreCase(userRole)) {
			addArrayContainsWithDomainPredicate(root, builder, predicates, DuePaymentsView_.MAINTAINER_IDS,
					DuePaymentsView_.DOMAIN_URL, filter.getUserId(), domainUrl);
		}

		// Add search filter for all fields
		addSearchLikePredicate(root, builder, predicates);

		// Add equality filters
		addEqualityPredicate(root, builder, predicates, DuePaymentsView_.COURSE_ID, filter.getProgramId());
		addEqualityPredicate(root, builder, predicates, DuePaymentsView_.SPORT, filter.getSport());
		addEqualityPredicate(root, builder, predicates, DuePaymentsView_.ACADEMY_ID, filter.getAcademyId());

		// Add "IN" filters for lists
		addInPredicate(root, builder, predicates, DuePaymentsView_.ACADEMY_ID, filter.getAcademyIds());
		addInPredicateIgnoreCase(root, builder, predicates, DuePaymentsView_.AGE_CATEGORY, filter.getAgeCategory());
		addInPredicate(root, builder, predicates, DuePaymentsView_.COURSE_ID, filter.getProgramIds());
		addInPredicateIgnoreCase(root, builder, predicates, DuePaymentsView_.SPORT, filter.getSports());

		// Date range filters (if needed)
//		if (filter.getStartDate() != null) {
//			addEqualityPredicate(root, builder, predicates, DuePaymentsView_.DUES_ON, filter.getStartDate());
//		}
//
//		if (filter.getEndDate() != null) {
//			predicates.add(builder.lessThanOrEqualTo(root.get(DuePaymentsView_.DUES_ON), filter.getEndDate()));
//		}

		// Add sorting
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}
}