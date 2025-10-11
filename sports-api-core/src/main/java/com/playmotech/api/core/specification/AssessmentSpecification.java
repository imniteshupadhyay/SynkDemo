package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.Academy_;
import com.playmotech.api.core.dao_postgres.Assessment;
import com.playmotech.api.core.dao_postgres.AssessmentAcademyMapping_;
import com.playmotech.api.core.dao_postgres.Assessment_;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class AssessmentSpecification implements Specification<Assessment> {

	private final transient GenericFilter filter;

	@Override
	public Predicate toPredicate(Root<Assessment> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		addSearchPredicate(root, builder, predicates);

		addAcademyInPredicate(root, builder, predicates);

		addDateTimeRangePredicate(root, builder, predicates);

		addInPredicate(root, builder, predicates, Assessment_.GENDER, filter.getGender());
		addInPredicate(root, builder, predicates, Assessment_.SPORT, filter.getSports());
		addInPredicate(root, builder, predicates, Assessment_.AGE_CATEGORY, filter.getAgeCategory());
		addInPredicate(root, builder, predicates, Assessment_.ASSESSMENT_STATUS, filter.getAssessment().getStatus());

		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private static <T> void addInPredicate(Root<Assessment> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private static <T> void addEqualityPredicate(Root<Assessment> root, CriteriaBuilder builder,
			List<Predicate> predicates, Path<T> path, T value) {
		if (value != null) {
			predicates.add(builder.equal(path, value));
		}
	}

	private void addSearchPredicate(Root<Assessment> root, CriteriaBuilder builder, List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";
			predicates.add(builder.or(builder.like(builder.lower(root.get(Assessment_.ASSESSMENT_TITLE)), search),
					builder.like(builder.lower(root.get(Assessment_.ASSESSMENT_DESCRIPTION)), search),
					builder.like(builder.lower(root.get(Assessment_.LOCATION)), search),
					builder.like(builder.lower(root.get(Assessment_.SPORT)), search),
					builder.like(builder.lower(root.get(Assessment_.GENDER)), search),
					builder.like(builder.lower(root.get(Assessment_.AGE_CATEGORY)), search)));
		}
	}

	private void addDateRangePredicate(Root<Assessment> root, CriteriaBuilder builder, List<Predicate> predicates) {
		if (filter.getStartDate() != null) {
			LocalDate start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			predicates
					.add(builder.greaterThanOrEqualTo(root.get(Assessment_.START_DATE), java.sql.Date.valueOf(start)));
		}
		if (filter.getEndDate() != null) {
			LocalDate end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			predicates.add(builder.lessThanOrEqualTo(root.get(Assessment_.END_DATE), java.sql.Date.valueOf(end)));
		}
	}

	private void addDateTimeRangePredicate(Root<Assessment> root, CriteriaBuilder builder, List<Predicate> predicates) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		if (filter.getStartDate() != null && filter.getEndDate() != null) {
			String startDateStr = filter.getStartDate().toLocalDateTime().format(formatter);
			String endDateStr = filter.getEndDate().toLocalDateTime().format(formatter);

			predicates.add(builder.and(builder.greaterThanOrEqualTo(root.get(Assessment_.END_DATE), startDateStr),
					builder.lessThanOrEqualTo(root.get(Assessment_.START_DATE), endDateStr)));
		} else if (filter.getStartDate() != null) {
			String startDateStr = filter.getStartDate().toLocalDateTime().format(formatter);
			predicates.add(builder.greaterThanOrEqualTo(root.get(Assessment_.END_DATE), startDateStr));
		} else if (filter.getEndDate() != null) {
			String endDateStr = filter.getEndDate().toLocalDateTime().format(formatter);
			predicates.add(builder.lessThanOrEqualTo(root.get(Assessment_.START_DATE), endDateStr));
		}
	}

	private void addAcademyInPredicate(Root<Assessment> root, CriteriaBuilder builder, List<Predicate> predicates) {
		List<String> academyIds = filter.getAcademyIds();
		if (academyIds != null && !academyIds.isEmpty()) {
			// Join to the academy mapping and academy table
			Predicate mappedAcademies = root.join(Assessment_.ACADEMY_MAPPINGS).join(AssessmentAcademyMapping_.ACADEMY)
					.get(Academy_.ID).in(academyIds);

			// Add forAllAcademies = true condition
			Predicate forAllAcademies = builder.isTrue(root.get(Assessment_.FOR_ALL_ACADEMIES));

			// Include assessments that match either condition
			predicates.add(builder.or(mappedAcademies, forAllAcademies));
		}
	}

	private void addOrderBy(Root<Assessment> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(Assessment_.CREATED_ON))
						: builder.desc(root.get(Assessment_.CREATED_ON)));
			}
		}
	}

	private boolean isValidOrderByColumn(String column) {
		return Assessment_.ASSESSMENT_TITLE.equals(column) || Assessment_.START_DATE.equals(column)
				|| Assessment_.END_DATE.equals(column) || Assessment_.CREATED_ON.equals(column)
				|| Assessment_.SPORT.equals(column) || Assessment_.AGE_CATEGORY.equals(column)
				|| Assessment_.GENDER.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<Assessment> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}
}
