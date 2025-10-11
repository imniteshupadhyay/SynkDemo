package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.EnrollmentPaymentsView;
import com.playmotech.api.core.views.EnrollmentPaymentsView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class EnrollmentPaymentViewSpecification implements Specification<EnrollmentPaymentsView> {

	private final transient GenericFilter filter;
	private final UserProfileHelper userProfileHelper;

	private static boolean isValidOrderByColumn(String column) {
		return EnrollmentPaymentsView_.TRAINEE_USER_ID.equals(column)
				|| EnrollmentPaymentsView_.COURSE_ID.equals(column) || EnrollmentPaymentsView_.ACADEMY_ID.equals(column)
				|| EnrollmentPaymentsView_.TOTAL_EXPECTED_PAYMENT.equals(column)
				|| EnrollmentPaymentsView_.TOTAL_PAID.equals(column)
				|| EnrollmentPaymentsView_.PENDING_AMOUNT.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<EnrollmentPaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private void addOrderBy(Root<EnrollmentPaymentsView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(EnrollmentPaymentsView_.ENROLLMENT_ID))
						: builder.desc(root.get(EnrollmentPaymentsView_.ENROLLMENT_ID)));
			}
		}
	}

	private void addSearchLikePredicate(Root<EnrollmentPaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			// Search across multiple fields
			predicates.add(builder.or(
					builder.like(builder.lower(root.get(EnrollmentPaymentsView_.TRAINEE_USER_ID).as(String.class)),
							search),
					builder.like(builder.lower(root.get(EnrollmentPaymentsView_.COURSE_ID).as(String.class)), search),
					builder.like(builder.lower(root.get(EnrollmentPaymentsView_.ACADEMY_ID).as(String.class)), search),
					builder.like(
							builder.lower(root.get(EnrollmentPaymentsView_.TOTAL_EXPECTED_PAYMENT).as(String.class)),
							search),
					builder.like(builder.lower(root.get(EnrollmentPaymentsView_.TOTAL_PAID).as(String.class)), search),
					builder.like(builder.lower(root.get(EnrollmentPaymentsView_.PENDING_AMOUNT).as(String.class)),
							search)));
		}
	}

	private void ownerSpecificStations(Root<EnrollmentPaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		predicates.add(builder.equal(root.get(EnrollmentPaymentsView_.MANAGER_ID), filter.getUserId()));
	}

	private static <T> void addInPredicate(Root<EnrollmentPaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private static void addInPredicateIgnoreCase(Root<EnrollmentPaymentsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<String> values) {
		if (values != null && !values.isEmpty()) {
			List<Predicate> inPredicates = new ArrayList<>();
			for (String value : values) {
				inPredicates.add(builder.equal(builder.lower(root.get(field)), value.toLowerCase(Locale.ENGLISH)));
			}
			predicates.add(builder.or(inPredicates.toArray(new Predicate[0])));
		}
	}

	@Override
	public Predicate toPredicate(Root<EnrollmentPaymentsView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();

		String domainUrl = filter.getDomainUrl();

		// Add search like predicate
		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			ownerSpecificStations(root, builder, predicates);
		} else if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, EnrollmentPaymentsView_.COACH_IDS,
					EnrollmentPaymentsView_.DOMAIN_URL, filter.getUserId(), domainUrl);

		} else if (RoleType.ADMIN.getRole().equalsIgnoreCase(userRole)
				|| RoleType.CLUSTER_HEAD.getRole().equalsIgnoreCase(userRole)
				|| RoleType.PROGRAM_MANAGER.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, EnrollmentPaymentsView_.MAINTAINER_IDS,
					EnrollmentPaymentsView_.DOMAIN_URL, filter.getUserId(), domainUrl);
		}

		// Add search filter for all fields
		addSearchLikePredicate(root, builder, predicates);

		// Add equality filters
//		addEqualityPredicate(root, builder, predicates, EnrollmentPaymentsView_.ACADEMY_ID, filter.getAcademyId());

		// Add "IN" filters
		addInPredicate(root, builder, predicates, EnrollmentPaymentsView_.ACADEMY_ID, filter.getAcademyIds());
		addInPredicate(root, builder, predicates, EnrollmentPaymentsView_.BRANCH_ID, filter.getBranchIds());
		addInPredicate(root, builder, predicates, EnrollmentPaymentsView_.COURSE_ID, filter.getProgramIds());
		addInPredicate(root, builder, predicates, EnrollmentPaymentsView_.SPORT, filter.getSports());
		addInPredicate(root, builder, predicates, EnrollmentPaymentsView_.AGE_CATEGORY, filter.getAgeCategory());
		// Add sorting
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addInPredicate(Root<EnrollmentPaymentsView> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String userId) {
		predicates.add(
				builder.like(builder.function("array_to_string", String.class, root.get(field), builder.literal(",")),
						"%" + userId + "%"));
	}

	private void addInPredicate(Root<EnrollmentPaymentsView> root, CriteriaBuilder builder, List<Predicate> predicates,
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
