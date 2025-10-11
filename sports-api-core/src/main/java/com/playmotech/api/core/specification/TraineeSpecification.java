package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.TraineeView;
import com.playmotech.api.core.views.TraineeView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class TraineeSpecification implements Specification<TraineeView> {

	private static final long serialVersionUID = 1L;

	private final transient GenericFilter filter;
	private final transient UserProfile currentUser;
	private final CoachAcademyMappingRepo coachAcademyMappingRepo;
	private final transient TraineeAcademyMappingRepo traineeAcademyMappingRepo;

	private static <T> void addEqualityPredicate(Root<TraineeView> root, CriteriaBuilder builder,
			List<Predicate> predicates, Path<T> path, T value) {
		if (value != null) {
			predicates.add(builder.equal(path, value));
		}
	}

	private void addSearchLikePredicate(Root<TraineeView> root, CriteriaBuilder builder, List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			predicates.add(builder.or(builder.like(builder.lower(root.get(TraineeView_.DISPLAY_NAME)), search),
					builder.like(builder.lower(root.get(TraineeView_.DOB)), search),
					builder.like(builder.lower(root.get(TraineeView_.EMAIL_ID)), search),
					builder.like(builder.lower(root.get(TraineeView_.PHONE_NUMBER)), search),
					builder.like(builder.lower(root.get(TraineeView_.ABOUT_ME)), search)));
		}
	}

	private static <T> void addInPredicate(Root<TraineeView> root, List<Predicate> predicates, Path<T> path,
			List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(path.in(values));
		}
	}

	private static void addInPredicateIgnoreCase(Root<TraineeView> root, CriteriaBuilder builder,
			List<Predicate> predicates, Path<String> path, List<String> values) {
		if (values != null && !values.isEmpty()) {
			List<Predicate> inPredicates = new ArrayList<>();
			for (String value : values) {
				inPredicates.add(builder.equal(builder.lower(path), value.toLowerCase()));
			}
			predicates.add(builder.or(inPredicates.toArray(new Predicate[0])));
		}
	}

	private void addOrderBy(Root<TraineeView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			Path<?> path = switch (orderBy) {
			case "displayName" -> root.get(TraineeView_.DISPLAY_NAME);
			case "dob" -> root.get(TraineeView_.DOB);
			case "gender" -> root.get(TraineeView_.GENDER);
			case "emailId" -> root.get(TraineeView_.EMAIL_ID);
			case "phoneNumber" -> root.get(TraineeView_.PHONE_NUMBER);
			case "about" -> root.get(TraineeView_.ABOUT_ME);
			case "createdOn" -> root.get(TraineeView_.CREATED_ON);
			default -> root.get(TraineeView_.ID);
			};
			query.orderBy(filter.isAscending() ? builder.asc(path) : builder.desc(path));
		}
	}

	@Override
	public Predicate toPredicate(Root<TraineeView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();
		String domainUrl = filter.getDomainUrl();

		// Role-specific predicates
		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			addArrayContainsPredicate(root, builder, predicates, TraineeView_.MANAGER_USER_IDS, filter.getUserId());
		} else if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addArrayContainsPredicate(root, builder, predicates, TraineeView_.COACH_USER_IDS, filter.getUserId());
		} else if (RoleType.ADMIN.getRole().equalsIgnoreCase(userRole)
				|| RoleType.CLUSTER_HEAD.getRole().equalsIgnoreCase(userRole)
				|| RoleType.PROGRAM_MANAGER.getRole().equalsIgnoreCase(userRole)) {
			addArrayContainsPredicate(root, builder, predicates, TraineeView_.MAINTAINER_IDS, filter.getUserId());
		}

		// Add domain URL filter for all roles except ACADEMY_OWNER
		if (!RoleType.SUPER_ADMIN.getRole().equalsIgnoreCase(userRole)) {
			addArrayContainsPredicate(root, builder, predicates, TraineeView_.DOMAIN_URLS, domainUrl);
		}

		addSearchLikePredicate(root, builder, predicates);

		addCreatedBetweenPredicate(root, builder, predicates);

		// Common filters
//		addArrayContainsPredicate(root, builder, predicates, TraineeView_.ACADEMY_IDS, filter.getAcademyId());
//		addEqualityPredicate(root, builder, predicates, root.get(TraineeView_.ACADEMY_IDS), filter.getAcademyId());
		addArrayContainsPredicate(root, builder, predicates, TraineeView_.ACADEMY_IDS, filter.getAcademyIds());
		addArrayContainsPredicate(root, builder, predicates, TraineeView_.COURSE_IDS, filter.getProgramIds());

		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addCreatedBetweenPredicate(Root<TraineeView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (filter.getStartDate() != null || filter.getEndDate() != null) {
			Path<Timestamp> createdOnPath = root.get(TraineeView_.CREATED_ON);

			if (filter.getStartDate() != null && filter.getEndDate() != null) {
				// Both start and end timestamps are provided
				predicates.add(builder.between(createdOnPath, filter.getStartDate(), filter.getEndDate()));
			} else if (filter.getStartDate() != null) {
				// Only start timestamp is provided
				predicates.add(builder.greaterThanOrEqualTo(createdOnPath, filter.getStartDate()));
			} else {
				// Only end timestamp is provided
				predicates.add(builder.lessThanOrEqualTo(createdOnPath, filter.getEndDate()));
			}
		}
	}

	private void addArrayContainsPredicate(Root<TraineeView> root, CriteriaBuilder builder, List<Predicate> predicates,
			String attribute, List<String> values) {

		if (values != null && !values.isEmpty()) {
			List<Predicate> orPredicates = new ArrayList<>();

			for (String value : values) {
				if (isStringNotNullAndBlank.test(value)) {
					Predicate matchPredicate = builder.like(builder.function("array_to_string", String.class,
							root.get(attribute), builder.literal(",")), "%" + value + "%");
					orPredicates.add(matchPredicate);
				}
			}

			if (!orPredicates.isEmpty()) {
				predicates.add(builder.or(orPredicates.toArray(new Predicate[0])));
			}
		}
	}

	private void addArrayContainsPredicate(Root<TraineeView> root, CriteriaBuilder builder, List<Predicate> predicates,
			String attribute, String matchValue) {

		if (isStringNotNullAndBlank.test(matchValue)) {
			Predicate matchPredicate = builder.like(
					builder.function("array_to_string", String.class, root.get(attribute), builder.literal(",")),
					"%" + matchValue + "%");
			predicates.add(matchPredicate);
		}
	}

}
