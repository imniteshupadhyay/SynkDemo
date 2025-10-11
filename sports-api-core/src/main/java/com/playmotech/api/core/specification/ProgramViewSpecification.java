package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.CourseDetailsView;
import com.playmotech.api.core.views.CourseDetailsView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class ProgramViewSpecification implements Specification<CourseDetailsView> {

	private final transient GenericFilter filter;

	private static boolean isValidOrderByColumn(String column) {
		return CourseDetailsView_.TITLE.equals(column) || CourseDetailsView_.DESCRIPTION.equals(column)
				|| CourseDetailsView_.SPORT.equals(column) || CourseDetailsView_.ACADEMY_ID.equals(column)
				|| CourseDetailsView_.AGE_CATEGORY.equals(column) || CourseDetailsView_.DOMAIN_URL.equals(column)
				|| CourseDetailsView_.START_DATE.equals(column) || CourseDetailsView_.END_DATE.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, Path<T> path, T value) {
		if (value != null) {
			predicates.add(builder.equal(path, value));
		}
	}

	private void addOrderBy(Root<CourseDetailsView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			Path<?> path = switch (orderBy) {
			case "title" -> root.get(CourseDetailsView_.TITLE);
			case "description" -> root.get(CourseDetailsView_.DESCRIPTION);
			case "sport" -> root.get(CourseDetailsView_.SPORT);
			case "academyId" -> root.get(CourseDetailsView_.ACADEMY_ID);
			case "ageCategory" -> root.get(CourseDetailsView_.AGE_CATEGORY);
			case "scheduleType" -> root.get(CourseDetailsView_.SCHEDULE_TYPE);
			case "domainUrl" -> root.get(CourseDetailsView_.DOMAIN_URL);
			case "startDate" -> root.get(CourseDetailsView_.START_DATE);
			case "endDate" -> root.get(CourseDetailsView_.END_DATE);
			case "createdOn" -> root.get(CourseDetailsView_.CREATED_ON);
			default -> root.get(CourseDetailsView_.CREATED_ON);
			};
			query.orderBy(filter.isAscending() ? builder.asc(path) : builder.desc(path));
		}
	}

	private void ownerSpecificCourses(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		predicates.add(builder.equal(root.get(CourseDetailsView_.MANAGER_USER_ID), filter.getUserId()));
	}

	private void addSearchLikePredicate(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

			predicates.add(builder.or(builder.like(builder.lower(root.get(CourseDetailsView_.TITLE)), search),
					builder.like(builder.lower(root.get(CourseDetailsView_.DESCRIPTION)), search),
					builder.like(builder.lower(root.get(CourseDetailsView_.SPORT)), search),
					builder.like(builder.lower(root.get(CourseDetailsView_.SCHEDULE_TYPE)), search),
					builder.like(builder.lower(root.get(CourseDetailsView_.DOMAIN_URL)), search),
					builder.like(builder.lower(root.get(CourseDetailsView_.AGE_CATEGORY)), search)));
		}
	}

	private void addDateTimeRangePredicate(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		if (filter.getStartDate() != null && filter.getEndDate() != null) {
			String startDateStr = filter.getStartDate().toLocalDateTime().format(formatter);
			String endDateStr = filter.getEndDate().toLocalDateTime().format(formatter);

			predicates
					.add(builder.and(builder.greaterThanOrEqualTo(root.get(CourseDetailsView_.END_DATE), startDateStr),
							builder.lessThanOrEqualTo(root.get(CourseDetailsView_.START_DATE), endDateStr)));
		} else if (filter.getStartDate() != null) {
			String startDateStr = filter.getStartDate().toLocalDateTime().format(formatter);
			predicates.add(builder.greaterThanOrEqualTo(root.get(CourseDetailsView_.END_DATE), startDateStr));
		} else if (filter.getEndDate() != null) {
			String endDateStr = filter.getEndDate().toLocalDateTime().format(formatter);
			predicates.add(builder.lessThanOrEqualTo(root.get(CourseDetailsView_.START_DATE), endDateStr));
		}
	}

	private void addDateRangeOnSearch(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (filter.getStartDate() != null && filter.getEndDate() != null) {
			String startDate = filter.getStartDate().toLocalDateTime().toString();
			String endDate = filter.getEndDate().toLocalDateTime().toString();

			predicates.add(builder.or(
					// Courses that are active during the entire filter period
					builder.and(builder.lessThanOrEqualTo(root.get(CourseDetailsView_.START_DATE), startDate),
							builder.greaterThanOrEqualTo(root.get(CourseDetailsView_.END_DATE), endDate)),
					// Courses that start during the filter period
					builder.between(root.get(CourseDetailsView_.START_DATE), startDate, endDate),
					// Courses that end during the filter period
					builder.between(root.get(CourseDetailsView_.END_DATE), startDate, endDate),
					// Courses that are active during any part of the filter period
					builder.and(builder.lessThanOrEqualTo(root.get(CourseDetailsView_.START_DATE), endDate),
							builder.greaterThanOrEqualTo(root.get(CourseDetailsView_.END_DATE), startDate))));
		}
	}

	private static void addTimeSpanPredicate(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String indiaTimeZone) {
		LocalDateTime currentIndiaTime = LocalDateTime.now(ZoneId.of(indiaTimeZone));

		predicates.add(builder.and(builder.lessThanOrEqualTo(root.get(CourseDetailsView_.START_DATE), currentIndiaTime),
				builder.greaterThanOrEqualTo(root.get(CourseDetailsView_.END_DATE), currentIndiaTime)));
	}

	private static <T> void addInPredicate(Root<CourseDetailsView> root, List<Predicate> predicates, Path<T> path,
			List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(path.in(values));
		}
	}

	private void addCompletionStatusPredicate(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (filter.getCourseView() != null) {
			LocalDateTime currentIndiaTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
			String currentTimeString = currentIndiaTime.toString(); // Ensure DB uses ISO-8601 format

			if (filter.getCourseView().isCompleted()) {
				// Completed: course has already ended
				predicates.add(builder.lessThan(root.get(CourseDetailsView_.END_DATE), currentTimeString));
			} else {
				// Not completed: include both active and upcoming
				Predicate active = builder.and(
						builder.lessThanOrEqualTo(root.get(CourseDetailsView_.START_DATE), currentTimeString),
						builder.greaterThanOrEqualTo(root.get(CourseDetailsView_.END_DATE), currentTimeString));

				Predicate upcoming = builder.greaterThan(root.get(CourseDetailsView_.START_DATE), currentTimeString);

				predicates.add(builder.or(active, upcoming));
			}
		}
	}

	private static void addInPredicateIgnoreCase(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, Path<String> path, List<String> values) {
		if (values != null && !values.isEmpty()) {
			List<Predicate> inPredicates = new ArrayList<>();
			for (String value : values) {
				inPredicates.add(builder.equal(builder.lower(path), value.toLowerCase(Locale.ENGLISH)));
			}
			predicates.add(builder.or(inPredicates.toArray(new Predicate[0])));
		}
	}

	@Override
	public Predicate toPredicate(Root<CourseDetailsView> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();
		String domainUrl = filter.getDomainUrl();

		if (RoleType.ACADEMY_OWNER.getRole().equalsIgnoreCase(userRole)) {
			ownerSpecificCourses(root, builder, predicates);
		} else if (RoleType.COACH.getRole().equalsIgnoreCase(userRole)) {
			addCoachUserPredicate(root, builder, predicates, filter.getUserId(), domainUrl);
		} else if (RoleType.ADMIN.getRole().equalsIgnoreCase(userRole)
				|| RoleType.CLUSTER_HEAD.getRole().equalsIgnoreCase(userRole)
				|| RoleType.PROGRAM_MANAGER.getRole().equalsIgnoreCase(userRole)) {
			addMaintainerUserPredicate(root, builder, predicates, filter.getUserId(), domainUrl);
		}

		addSearchLikePredicate(root, builder, predicates);

		addEqualityPredicate(root, builder, predicates, root.get(CourseDetailsView_.ACADEMY_ID), filter.getAcademyId());

		addInPredicate(root, predicates, root.get(CourseDetailsView_.SPORT), filter.getSports());

		addInPredicate(root, predicates, root.get(CourseDetailsView_.ACADEMY_ID), filter.getAcademyIds());
		addInPredicate(root, predicates, root.get(CourseDetailsView_.AGE_CATEGORY), filter.getAgeCategory());

		addInPredicateIgnoreCase(root, builder, predicates, root.get(CourseDetailsView_.AGE_CATEGORY),
				filter.getAgeCategory());

		// DATE-TIME PREDICATES - ADD THEM HERE
		// Option 1: For strict date range filtering (courses must be completely within
		// the filter range)
		addDateTimeRangePredicate(root, builder, predicates);

		addCompletionStatusPredicate(root, builder, predicates);

		// Option 2: For overlapping date range filtering (courses that overlap with
		// filter range in any way)
		// addDateRangeOnSearch(root, builder, predicates);

		// Option 3: For current active courses (based on India timezone)
		// addTimeSpanPredicate(root, builder, predicates, "Asia/Kolkata");

		// Note: Typically you would use only one of the above date predicates based on
		// requirements
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addCoachUserPredicate(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String userId, String domainUrl) {
		Predicate coachMatch = builder.like(builder.function("array_to_string", String.class,
				root.get(CourseDetailsView_.COACH_USER_IDS), builder.literal(",")), "%" + userId + "%");
		Predicate domainMatch = builder.equal(root.get(CourseDetailsView_.DOMAIN_URL), domainUrl);
		predicates.add(builder.and(coachMatch, domainMatch));
	}

	private void addMaintainerUserPredicate(Root<CourseDetailsView> root, CriteriaBuilder builder,
			List<Predicate> predicates, String userId, String domainUrl) {
		Predicate coachMatch = builder.like(builder.function("array_to_string", String.class,
				root.get(CourseDetailsView_.MAINTAINER_IDS), builder.literal(",")), "%" + userId + "%");
		Predicate domainMatch = builder.equal(root.get(CourseDetailsView_.DOMAIN_URL), domainUrl);
		predicates.add(builder.and(coachMatch, domainMatch));
	}
}
