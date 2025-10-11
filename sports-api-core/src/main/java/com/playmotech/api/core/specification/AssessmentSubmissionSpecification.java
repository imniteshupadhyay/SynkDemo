package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.Academy_;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration_;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission_;
import com.playmotech.api.core.dao_postgres.Assessment_;
import com.playmotech.api.core.dao_postgres.UserProfile_;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class AssessmentSubmissionSpecification implements Specification<AssessmentPlayerSubmission> {

	private final transient GenericFilter filter;
	private final List<String> academyIds;

	@Override
	public Predicate toPredicate(Root<AssessmentPlayerSubmission> root, CriteriaQuery<?> query,
			CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		if (academyIds != null) {
			predicates.add(root.get(AssessmentPlayerSubmission_.ACADEMY).get(Academy_.ID).in(academyIds));
		}

		if (filter.getAssessmentPlayerSubmission() != null
				&& filter.getAssessmentPlayerSubmission().getAssessmentId() != null) {
			List<String> assessmentIds = new ArrayList<>();
			assessmentIds.add(filter.getAssessmentPlayerSubmission().getAssessmentId());
			addInPredicate(root.get(AssessmentPlayerSubmission_.ASSESSMENT).get(Assessment_.ID), predicates,
					assessmentIds);
		}

		addInPredicate(root.get(AssessmentPlayerSubmission_.ACADEMY).get(Academy_.ID), predicates,
				filter.getAcademyIds());
		addInPredicate(root, builder, predicates, AssessmentPlayerSubmission_.SUBMISSION_STATUS,
				filter.getAssessmentPlayerSubmission().getStatus());

		addDateRangePredicate(root, builder, predicates);
		addSearchLikePredicate(root, builder, predicates);

		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private static <T> void addInPredicate(Root<AssessmentPlayerSubmission> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

	private static <T> void addInPredicate(Path<T> path, List<Predicate> predicates, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(path.in(values));
		}
	}

	private void addSearchLikePredicate(Root<AssessmentPlayerSubmission> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			predicates.add(builder.or(
					builder.like(builder.lower(root.get(AssessmentPlayerSubmission_.SUBMISSION_STATUS)), search),

					builder.like(builder.lower(root.join(AssessmentPlayerSubmission_.REGISTRATION)
							.get(AssessmentPlayerRegistration_.REGISTRATION_NUMBER)), search),
					builder.like(
							builder.lower(root.join(AssessmentPlayerSubmission_.PLAYER).get(UserProfile_.DISPLAY_NAME)),
							search),
					builder.like(
							builder.lower(root.join(AssessmentPlayerSubmission_.PLAYER).get(UserProfile_.PHONE_NUMBER)),
							search)));
		}
	}

	private void addDateRangePredicate(Root<AssessmentPlayerSubmission> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (filter.getStartDate() != null) {
			LocalDate start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			predicates.add(builder.greaterThanOrEqualTo(root.get(AssessmentPlayerSubmission_.CREATED_ON),
					Timestamp.valueOf(start.atStartOfDay())));
		}
		if (filter.getEndDate() != null) {
			LocalDate end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			predicates.add(builder.lessThanOrEqualTo(root.get(AssessmentPlayerSubmission_.CREATED_ON),
					Timestamp.valueOf(end.plusDays(1).atStartOfDay())));
		}
	}

	private void addOrderBy(Root<AssessmentPlayerSubmission> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(AssessmentPlayerSubmission_.CREATED_ON))
						: builder.desc(root.get(AssessmentPlayerSubmission_.CREATED_ON)));
			}
		}
	}

	private boolean isValidOrderByColumn(String column) {
		return AssessmentPlayerSubmission_.CREATED_ON.equals(column)
				|| AssessmentPlayerSubmission_.GENDER.equals(column)
				|| AssessmentPlayerSubmission_.SUBMISSION_STATUS.equals(column);
	}
}
