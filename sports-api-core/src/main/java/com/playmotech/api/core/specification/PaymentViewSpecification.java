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
import com.playmotech.api.core.views.PaymentDetailsView;
import com.playmotech.api.core.views.PaymentDetailsView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class PaymentViewSpecification implements Specification<PaymentDetailsView> {

	private final transient GenericFilter filter;

	private final transient UserProfileHelper userProfileHelper;

	private static boolean isValidOrderByColumn(String column) {
		return PaymentDetailsView_.PLAYER_NAME.equals(column) || PaymentDetailsView_.ACADEMY.equals(column)
				|| PaymentDetailsView_.BRANCH.equals(column) || PaymentDetailsView_.SPORT.equals(column)
				|| PaymentDetailsView_.RECEIPT_ID.equals(column) || PaymentDetailsView_.AMOUNT.equals(column)
				|| PaymentDetailsView_.PAYMENT_MODE.equals(column) || PaymentDetailsView_.PAYMENT_STATUS.equals(column)
				|| PaymentDetailsView_.PAYMENT_INITIATED_BY_USER_ID.equals(column)
				|| PaymentDetailsView_.CURRENCY.equals(column) || PaymentDetailsView_.PROGRAM.equals(column)
				|| PaymentDetailsView_.CREATED_AT.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<PaymentDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private void addOrderBy(Root<PaymentDetailsView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(PaymentDetailsView_.CREATED_AT))
						: builder.desc(root.get(PaymentDetailsView_.CREATED_AT)));
			}
		}
	}

	private void addSearchLikePredicate(Root<PaymentDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			// Search across multiple fields
			predicates.add(builder.or(builder.like(builder.lower(root.get(PaymentDetailsView_.PLAYER_NAME)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.ACADEMY)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.BRANCH)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.SPORT)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.RECEIPT_ID)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.ENROLL_ID)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.AMOUNT).as(String.class)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.PAYMENT_MODE)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.PAYMENT_STATUS)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.CURRENCY)), search),
					builder.like(builder.lower(root.get(PaymentDetailsView_.PROGRAM)), search)));
		}
	}

//	private void addMarkedAtDateFilter(Root<PaymentDetailsView> root, CriteriaBuilder builder,
//			List<Predicate> predicates) {
//		if (filter.getDays() != null && filter.getDays() > 0) {
//			LocalDate fromDate = LocalDate.now().minusDays(filter.getDays());
//			predicates.add(builder.greaterThanOrEqualTo(root.get(PaymentDetailsView_.CREATED_AT), fromDate));
//		}
//	}

	private void ownerSpecificStations(Root<PaymentDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		predicates.add(builder.equal(root.get(PaymentDetailsView_.MANAGER_ID), filter.getUserId()));
	}

	private void addDateTimeRangePredicate(Root<PaymentDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
// Convert java.util.Date or Timestamp to LocalDateTime
		if (filter.getStartDate() != null) {
			LocalDateTime start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.greaterThanOrEqualTo(root.get(PaymentDetailsView_.CREATED_AT), start));
		}

		if (filter.getEndDate() != null) {
			LocalDateTime end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.lessThanOrEqualTo(root.get(PaymentDetailsView_.CREATED_AT), end));
		}
	}

	private static <T> void addInPredicate(Root<PaymentDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private static void addInPredicateIgnoreCase(Root<PaymentDetailsView> root, CriteriaBuilder builder,
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
	public Predicate toPredicate(Root<PaymentDetailsView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();

		String domainUrl = filter.getDomainUrl();

		// Add search like predicate
		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			ownerSpecificStations(root, builder, predicates);
		}
		if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, PaymentDetailsView_.COACH_IDS, PaymentDetailsView_.DOMAIN_URL,
					filter.getUserId(), domainUrl);

		}
		if (RoleType.ADMIN.getRole().equalsIgnoreCase(userRole)
				|| RoleType.CLUSTER_HEAD.getRole().equalsIgnoreCase(userRole)
				|| RoleType.PROGRAM_MANAGER.getRole().equalsIgnoreCase(userRole)) {
			addInPredicate(root, builder, predicates, PaymentDetailsView_.MAINTAINER_IDS,
					PaymentDetailsView_.DOMAIN_URL, filter.getUserId(), domainUrl);
		}

		// Add search filter for all fields
		addSearchLikePredicate(root, builder, predicates);

		// Add equality filters
//		addEqualityPredicate(root, builder, predicates, PaymentDetailsView_.ACADEMY_ID, filter.getAcademyId());
		addEqualityPredicate(root, builder, predicates, PaymentDetailsView_.BRANCH_ID, filter.getBranchId());
		addEqualityPredicate(root, builder, predicates, PaymentDetailsView_.PROGRAM_ID, filter.getProgramId());
		addEqualityPredicate(root, builder, predicates, PaymentDetailsView_.SPORT, filter.getSport());
		addEqualityPredicate(root, builder, predicates, PaymentDetailsView_.PAYMENT_CATEGORY,
				filter.getPaymentCategory());

		// Add "IN" filters for branchIds and programIds
		addInPredicate(root, builder, predicates, PaymentDetailsView_.BRANCH_ID, filter.getBranchIds());
		addInPredicate(root, builder, predicates, PaymentDetailsView_.ACADEMY_ID, filter.getAcademyIds());

		addInPredicate(root, builder, predicates, PaymentDetailsView_.PLAYER_ID, filter.getPlayerIds());

		if (filter.getCoachId() != null && !filter.getCoachId().isEmpty()) {
			addInPredicate(root, builder, predicates, PaymentDetailsView_.COACH_IDS, filter.getCoachId());
		}

		addInPredicateIgnoreCase(root, builder, predicates, PaymentDetailsView_.AGE_CATEGORY, filter.getAgeCategory());
		addInPredicate(root, builder, predicates, PaymentDetailsView_.PROGRAM_ID, filter.getProgramIds());
		addInPredicateIgnoreCase(root, builder, predicates, PaymentDetailsView_.SPORT, filter.getSports());

		// time range filter
		addDateTimeRangePredicate(root, builder, predicates);

		// Add sorting
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addInPredicate(Root<PaymentDetailsView> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String userId) {
		predicates.add(
				builder.like(builder.function("array_to_string", String.class, root.get(field), builder.literal(",")),
						"%" + userId + "%"));
	}

	private void addInPredicate(Root<PaymentDetailsView> root, CriteriaBuilder builder, List<Predicate> predicates,
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
