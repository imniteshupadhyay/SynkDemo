package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.PendingPaymentDueView;
import com.playmotech.api.core.dao_postgres.PendingPaymentDueView_;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class PendingPaymentDueViewSpecification implements Specification<PendingPaymentDueView> {

	private final transient GenericFilter filter;

	private final transient UserProfileHelper userProfileHelper;

	private static boolean isValidOrderByColumn(String column) {
		return PendingPaymentDueView_.PLAYER_NAME.equals(column) || PendingPaymentDueView_.ACADEMY_NAME.equals(column)
				|| PendingPaymentDueView_.SPORT.equals(column)
				|| PendingPaymentDueView_.PENDING_COURSE_FEE.equals(column)
				|| PendingPaymentDueView_.PAYMENT_SCHEDULE.equals(column)
				|| PendingPaymentDueView_.ENROLLMENT_ID.equals(column)
				|| PendingPaymentDueView_.COURSE_NAME.equals(column)
				|| PendingPaymentDueView_.PLAYER_EMAIL_ID.equals(column)
				|| PendingPaymentDueView_.PLAYER_CONTACT_NUMBER.equals(column)
				|| PendingPaymentDueView_.DUE_DAYS_COUNT.equals(column)
				|| PendingPaymentDueView_.PENDING_REGISTRATION_FEE.equals(column)
				|| PendingPaymentDueView_.UPCOMING_AMOUNT.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<PendingPaymentDueView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private void addOrderBy(Root<PendingPaymentDueView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(PendingPaymentDueView_.PLAYER_NAME))
						: builder.desc(root.get(PendingPaymentDueView_.PLAYER_NAME)));
			}
		}
	}

	private void addSearchLikePredicate(Root<PendingPaymentDueView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			// Search across multiple fields
			predicates.add(builder.or(builder.like(builder.lower(root.get(PendingPaymentDueView_.PLAYER_NAME)), search),
					builder.like(builder.lower(root.get(PendingPaymentDueView_.ACADEMY_NAME)), search),
					builder.like(builder.lower(root.get(PendingPaymentDueView_.SPORT)), search),
					builder.like(builder.lower(root.get(PendingPaymentDueView_.ENROLLMENT_ID)), search),
					builder.like(
							builder.lower(root.get(PendingPaymentDueView_.PENDING_REGISTRATION_FEE).as(String.class)),
							search),
					builder.like(builder.lower(root.get(PendingPaymentDueView_.PENDING_COURSE_FEE).as(String.class)),
							search),
					builder.like(builder.lower(root.get(PendingPaymentDueView_.UPCOMING_AMOUNT).as(String.class)),
							search),
					builder.like(builder.lower(root.get(PendingPaymentDueView_.PLAYER_NAME)), search),
					builder.like(builder.lower(root.get(PendingPaymentDueView_.PLAYER_EMAIL_ID)), search),
					builder.like(builder.lower(root.get(PendingPaymentDueView_.PLAYER_CONTACT_NUMBER)), search),
					builder.like(builder.lower(root.get(PendingPaymentDueView_.COURSE_NAME)), search)));
		}
	}

//	private void addMarkedAtDateFilter(Root<PaymentDetailsView> root, CriteriaBuilder builder,
//			List<Predicate> predicates) {
//		if (filter.getDays() != null && filter.getDays() > 0) {
//			LocalDate fromDate = LocalDate.now().minusDays(filter.getDays());
//			predicates.add(builder.greaterThanOrEqualTo(root.get(PaymentDetailsView_.CREATED_AT), fromDate));
//		}
//	}

	private void ownerSpecificStations(Root<PendingPaymentDueView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		predicates.add(builder.equal(root.get(PendingPaymentDueView_.MANAGER_ID), filter.getUserId()));
	}

//	private void addDateTimeRangePredicate(Root<PaymentDetailsView> root, CriteriaBuilder builder,
//			List<Predicate> predicates) {
//// Convert java.util.Date or Timestamp to LocalDateTime
//		if (filter.getStartDate() != null) {
//			LocalDateTime start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//			predicates.add(builder.greaterThanOrEqualTo(root.get(PaymentDetailsView_.CREATED_AT), start));
//		}
//
//		if (filter.getEndDate() != null) {
//			LocalDateTime end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//			predicates.add(builder.lessThanOrEqualTo(root.get(PaymentDetailsView_.CREATED_AT), end));
//		}
//	}

	private static <T> void addInPredicate(Root<PendingPaymentDueView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private static void addInPredicateIgnoreCase(Root<PendingPaymentDueView> root, CriteriaBuilder builder,
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
	public Predicate toPredicate(Root<PendingPaymentDueView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();

		String domainUrl = filter.getDomainUrl();

		// Add search like predicate
		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			ownerSpecificStations(root, builder, predicates);
		}
		if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, PendingPaymentDueView_.COACH_IDS,
					PendingPaymentDueView_.DOMAIN_URL, filter.getUserId(), domainUrl);

		}
		if (RoleType.ADMIN.getRole().equalsIgnoreCase(userRole)
				|| RoleType.CLUSTER_HEAD.getRole().equalsIgnoreCase(userRole)
				|| RoleType.PROGRAM_MANAGER.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, PendingPaymentDueView_.MAINTAINER_IDS,
					PendingPaymentDueView_.DOMAIN_URL, filter.getUserId(), domainUrl);
		}

		// Add search filter for all fields
		addSearchLikePredicate(root, builder, predicates);

		// Add equality filters
//		addEqualityPredicate(root, builder, predicates, PaymentDetailsView_.ACADEMY_ID, filter.getAcademyId());
//		addEqualityPredicate(root, builder, predicates, PaymentDetailsView_.BRANCH_ID, filter.getBranchId());
		addEqualityPredicate(root, builder, predicates, PendingPaymentDueView_.COURSE_ID, filter.getProgramId());
		addEqualityPredicate(root, builder, predicates, PendingPaymentDueView_.SPORT, filter.getSport());
//		addEqualityPredicate(root, builder, predicates, PendingPaymentDueView_., filter.getPaymentCategory());

		// Add "IN" filters for branchIds and programIds
//		addInPredicate(root, builder, predicates, PaymentDetailsView_.BRANCH_ID, filter.getBranchIds());
		addInPredicate(root, builder, predicates, PendingPaymentDueView_.ACADEMY_ID, filter.getAcademyIds());
		addInPredicateIgnoreCase(root, builder, predicates, PendingPaymentDueView_.AGE_CATEGORY,
				filter.getAgeCategory());
		addInPredicate(root, builder, predicates, PendingPaymentDueView_.COURSE_ID, filter.getProgramIds());
		addInPredicateIgnoreCase(root, builder, predicates, PendingPaymentDueView_.SPORT, filter.getSports());

		// time range filter
//		addDateTimeRangePredicate(root, builder, predicates);

		// Add sorting
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addInPredicate(Root<PendingPaymentDueView> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String userId) {
		predicates.add(
				builder.like(builder.function("array_to_string", String.class, root.get(field), builder.literal(",")),
						"%" + userId + "%"));
	}

	private void addInPredicate(Root<PendingPaymentDueView> root, CriteriaBuilder builder, List<Predicate> predicates,
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
