package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.Activity;
import com.playmotech.api.core.dao_postgres.Activity_;
import com.playmotech.api.core.dao_postgres.Organisation_;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class ActivitySpecification implements Specification<Activity> {

	private final transient GenericFilter filter;

	private static boolean isValidOrderByColumn(String column) {
		return Activity_.NAME.equals(column) || Activity_.CATEGORY.equals(column)
				|| Activity_.SUBCATEGORY.equals(column) || Activity_.INSERTED_ON.equals(column);
	}

	private void addOrderBy(Root<Activity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(Activity_.INSERTED_ON))
						: builder.desc(root.get(Activity_.INSERTED_ON)));
			}
		}
	}

	private void addSearchLikePredicate(Root<Activity> root, CriteriaBuilder builder, List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";
			predicates.add(builder.or(builder.like(builder.lower(root.get(Activity_.NAME)), search),
					builder.like(builder.lower(root.get(Activity_.SUBCATEGORY)), search),
					builder.like(builder.lower(root.get(Activity_.DESCRIPTION)), search)));
		}
	}

	private void addDateTimeRangePredicate(Root<Activity> root, CriteriaBuilder builder, List<Predicate> predicates) {
		if (filter.getStartDate() != null) {
			LocalDateTime start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.greaterThanOrEqualTo(root.get(Activity_.INSERTED_ON), start));
		}
		if (filter.getEndDate() != null) {
			LocalDateTime end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.lessThanOrEqualTo(root.get(Activity_.INSERTED_ON), end));
		}
	}

	@Override
	public Predicate toPredicate(Root<Activity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();
		if (!RoleType.SUPER_ADMIN.name().equalsIgnoreCase(userRole)) {
			addJoinInPredicate(root, builder, predicates, Activity_.ORGANISATION, Organisation_.DOMAIN_URL,
					filter.getDomainUrl());
		}

		addSearchLikePredicate(root, builder, predicates);

		addEqualityPredicate(root, builder, predicates, Activity_.DELETED, !filter.isNotDeleted());

		if (filter.getActivity().getCategory() != null) {
			addEqualityPredicate(root, builder, predicates, Activity_.CATEGORY, filter.getActivity().getCategory());
		}

		if (filter.getActivity().getSubcategory() != null) {
			addEqualityPredicate(root, builder, predicates, Activity_.SUBCATEGORY,
					filter.getActivity().getSubcategory());
		}

		// Date range
		addDateTimeRangePredicate(root, builder, predicates);

		// Sorting
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addJoinInPredicate(Root<Activity> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String id, String academyId) {
		if (academyId != null && !academyId.isEmpty()) {
			predicates.add(root.join(field).get(id).in(academyId));
		}
	}

	private void addJoinInPredicate(Root<Activity> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String id, List<String> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.join(field).get(id).in(values));
		}

	}

	private static <T> void addEqualityPredicate(Root<Activity> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}
}
