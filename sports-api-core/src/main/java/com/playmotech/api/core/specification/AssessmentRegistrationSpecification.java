package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration_;
import com.playmotech.api.core.dao_postgres.Assessment_;
import com.playmotech.api.core.dao_postgres.UserProfile_;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class AssessmentRegistrationSpecification implements Specification<AssessmentPlayerRegistration> {

	private static final long serialVersionUID = 1L;

	private final transient GenericFilter filter;

	private static <T> void addEqualityPredicate(Root<AssessmentPlayerRegistration> root,
			CriteriaBuilder builder,
			List<Predicate> predicates,
			Path<T> path,
			T value) {
		if (value != null) {
			predicates.add(builder.equal(path, value));
		}
	}

	private void addSearchLikePredicate(Root<AssessmentPlayerRegistration> root,
			CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			predicates.add(builder.or(
					builder.like(builder.lower(root.get(AssessmentPlayerRegistration_.REGISTRATION_NUMBER)), search),
					builder.like(
							builder.lower(
									root.get(AssessmentPlayerRegistration_.PLAYER).get(UserProfile_.DISPLAY_NAME)),
							search),
					builder.like(
							builder.lower(root.get(AssessmentPlayerRegistration_.PLAYER).get(UserProfile_.USERNAME)),
							search)));
		}
	}

	private void addDateBetweenPredicate(Root<AssessmentPlayerRegistration> root,
			CriteriaBuilder builder,
			List<Predicate> predicates) {
		LocalDate startDate = filter.getStartDate() != null ? filter.getStartDate().toLocalDateTime().toLocalDate()
				: null;
		LocalDate endDate = filter.getEndDate() != null ? filter.getEndDate().toLocalDateTime().toLocalDate() : null;

		Path<LocalDate> datePath = root.get(AssessmentPlayerRegistration_.REGISTRATION_DATE);

		if (startDate != null && endDate != null) {
			predicates.add(builder.between(datePath, startDate, endDate));
		} else if (startDate != null) {
			predicates.add(builder.greaterThanOrEqualTo(datePath, startDate));
		} else if (endDate != null) {
			predicates.add(builder.lessThanOrEqualTo(datePath, endDate));
		}
	}

	private void addOrderBy(Root<AssessmentPlayerRegistration> root,
			CriteriaQuery<?> query,
			CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			Path<?> path = switch (orderBy) {
				case "playerStatus" -> root.get(AssessmentPlayerRegistration_.PLAYER_STATUS);
				case "paymentStatus" -> root.get(AssessmentPlayerRegistration_.PAYMENT_STATUS);
				case "registrationDate" -> root.get(AssessmentPlayerRegistration_.REGISTRATION_DATE);
				case "registrationNumber" -> root.get(AssessmentPlayerRegistration_.REGISTRATION_NUMBER);
				case "createdOn" -> root.get(AssessmentPlayerRegistration_.CREATED_ON);
				case "updatedOn" -> root.get(AssessmentPlayerRegistration_.UPDATED_ON);
				default -> root.get(AssessmentPlayerRegistration_.REGISTRATION_ID);
			};

			query.orderBy(filter.isAscending() ? builder.asc(path) : builder.desc(path));
		}
	}

	private static <T> void addInPredicate(Path<T> path, List<Predicate> predicates, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(path.in(values));
		}
	}

	@Override
	public Predicate toPredicate(Root<AssessmentPlayerRegistration> root,
			CriteriaQuery<?> query,
			CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		if (filter.getAssessmentPlayerRegistration() != null) {
			var reg = filter.getAssessmentPlayerRegistration();

			if (reg.getAssessmentId() != null) {
				addEqualityPredicate(root, builder, predicates,
						root.get(AssessmentPlayerRegistration_.ASSESSMENT).get(Assessment_.ID),
						reg.getAssessmentId());
			}

			addInPredicate(root.get(AssessmentPlayerRegistration_.PLAYER_STATUS), predicates, reg.getPlayerStatuses());
			addInPredicate(root.get(AssessmentPlayerRegistration_.PAYMENT_STATUS), predicates,
					reg.getPaymentStatuses());
		}

		addInPredicate(root.get(AssessmentPlayerRegistration_.PLAYER).get(UserProfile_.ID), predicates,
				filter.getPlayerIds());

		addSearchLikePredicate(root, builder, predicates);
		addDateBetweenPredicate(root, builder, predicates);
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}
}
