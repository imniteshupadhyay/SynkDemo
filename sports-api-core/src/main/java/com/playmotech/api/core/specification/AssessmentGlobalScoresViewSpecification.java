package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.AssessmentGlobalScoresView;
import com.playmotech.api.core.views.AssessmentGlobalScoresView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class AssessmentGlobalScoresViewSpecification implements Specification<AssessmentGlobalScoresView> {

	private final transient GenericFilter filter;
	private final transient List<String> academyIds;

	@Override
	public Predicate toPredicate(Root<AssessmentGlobalScoresView> root, CriteriaQuery<?> query,
			CriteriaBuilder builder) {

		List<Predicate> predicates = new ArrayList<>();

		// Filter by academy if academyIds are provided
		if (academyIds != null && !academyIds.isEmpty()) {
			predicates.add(root.get(AssessmentGlobalScoresView_.ACADEMY_ID).in(academyIds));
		}

		addEqualityPredicate(root, builder, predicates, AssessmentGlobalScoresView_.SPORT, filter.getSport());

		addInPredicate(root, builder, predicates, AssessmentGlobalScoresView_.ACADEMY_ID, filter.getAcademyIds());

		addInPredicate(root, builder, predicates, AssessmentGlobalScoresView_.AGE_CATEGORY, filter.getAgeCategory());

		addInPredicate(root, builder, predicates, AssessmentGlobalScoresView_.GENDER, filter.getGender());

		// Add search predicate if search term is provided
		addSearchLikePredicate(root, builder, predicates);

		// Add ordering
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	// Helper method for IN predicates
	private static <T> void addInPredicate(Root<AssessmentGlobalScoresView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private static <T> void addEqualityPredicate(Root<AssessmentGlobalScoresView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private void addSearchLikePredicate(Root<AssessmentGlobalScoresView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			predicates.add(builder.or(
					builder.like(builder.lower(root.get(AssessmentGlobalScoresView_.REGISTRATION_NUMBER)), search),
					builder.like(builder.lower(root.get(AssessmentGlobalScoresView_.ACADEMY_NAME)), search),
					builder.like(builder.lower(root.get(AssessmentGlobalScoresView_.DISPLAY_NAME)), search),
					builder.like(builder.lower(root.get(AssessmentGlobalScoresView_.SPORT)), search),
					builder.like(builder.lower(root.get(AssessmentGlobalScoresView_.GENDER)), search),
					builder.like(builder.lower(root.get(AssessmentGlobalScoresView_.AGE_CATEGORY)), search)));
		}
	}

	private void addOrderBy(Root<AssessmentGlobalScoresView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				// Default ordering by assessment rank if invalid column is provided
				query.orderBy(builder.asc(root.get(AssessmentGlobalScoresView_.ASSESSMENT_RANK)));
			}
		} else {
			// Default ordering by assessment rank if no order is specified
			query.orderBy(builder.asc(root.get(AssessmentGlobalScoresView_.ASSESSMENT_RANK)));
		}
	}

	private boolean isValidOrderByColumn(String column) {
		return AssessmentGlobalScoresView_.REGISTRATION_NUMBER.equals(column)
				|| AssessmentGlobalScoresView_.DISPLAY_NAME.equals(column)
				|| AssessmentGlobalScoresView_.ACADEMY_NAME.equals(column)
				|| AssessmentGlobalScoresView_.SPORT.equals(column) || AssessmentGlobalScoresView_.GENDER.equals(column)
				|| AssessmentGlobalScoresView_.AGE_CATEGORY.equals(column)
				|| AssessmentGlobalScoresView_.TOTAL_SCORE.equals(column)
				|| AssessmentGlobalScoresView_.ASSESSMENT_RANK.equals(column);
	}
}
