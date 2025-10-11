package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.Academy_;
import com.playmotech.api.core.dao_postgres.Course_;
import com.playmotech.api.core.dao_postgres.UserProfile_;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.AnalysisStatus;
import com.playmotech.api.core.dao_postgres.VideoAnalytics_;
import com.playmotech.api.core.dao_postgres.VideoAnalyzer;
import com.playmotech.api.core.dao_postgres.VideoAnalyzer_;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class VideoAnalyzerSpecification implements Specification<VideoAnalyzer> {

	private final transient GenericFilter filter;

	private static boolean isValidOrderByColumn(String column) {
		return VideoAnalyzer_.TITLE.equals(column) || VideoAnalyzer_.MEDIA_URL.equals(column)
				|| VideoAnalyzer_.INSERTED_ON.equals(column);
	}

	private static <T> void addEqualityPredicate(Root<VideoAnalyzer> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, T value) {
		if (value != null) {
			predicates.add(builder.equal(root.get(field), value));
		}
	}

	private static void addAcaEqualityPredicate(Root<VideoAnalyzer> root, CriteriaBuilder builder,
			List<Predicate> predicates, String academyId) {
		if (academyId != null) {
			predicates.add(builder.equal(root.join(VideoAnalyzer_.ACADEMY).get(Academy_.ID), academyId));
		}
	}

	private void addOrderBy(Root<VideoAnalyzer> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			if (isValidOrderByColumn(orderBy)) {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
			} else {
				query.orderBy(filter.isAscending() ? builder.asc(root.get(VideoAnalyzer_.INSERTED_ON))
						: builder.desc(root.get(VideoAnalyzer_.INSERTED_ON)));
			}
		}
	}

	private void addSearchLikePredicate(Root<VideoAnalyzer> root, CriteriaBuilder builder, List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";
			predicates.add(builder.or(builder.like(builder.lower(root.get(VideoAnalyzer_.TITLE)), search),
					builder.like(builder.lower(root.get(VideoAnalyzer_.MEDIA_URL)), search)));
		}
	}

	private void addDateTimeRangePredicate(Root<VideoAnalyzer> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (filter.getStartDate() != null) {
			LocalDateTime start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.greaterThanOrEqualTo(root.get(VideoAnalyzer_.INSERTED_ON), start));
		}

		if (filter.getEndDate() != null) {
			LocalDateTime end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			predicates.add(builder.lessThanOrEqualTo(root.get(VideoAnalyzer_.INSERTED_ON), end));
		}
	}

	private void addCoachSpecificPredicates(Root<VideoAnalyzer> root, CriteriaBuilder builder,
			List<Predicate> predicates) {

		predicates.add(builder.equal(root.join(VideoAnalyzer_.CREATED_BY).get(UserProfile_.ID), filter.getUserId()));

	}

	private void addPlayerSpecificPredicates(Root<VideoAnalyzer> root, CriteriaBuilder builder,
			List<Predicate> predicates) {

		predicates.add(builder.and(builder.isNotNull(root.join(VideoAnalyzer_.PLAYER).get(UserProfile_.ID)),
				builder.equal(root.join(VideoAnalyzer_.PLAYER).get(UserProfile_.ID), filter.getUserId())));

	}

	private void addAnalysisStatusPredicate(Root<VideoAnalyzer> root, CriteriaBuilder builder,
			List<Predicate> predicates) {
		if (isStringNotNullAndBlank.test(filter.getAnalysisStatus())) {
			try {
				AnalysisStatus status = AnalysisStatus.valueOf(filter.getAnalysisStatus().toUpperCase());
				
				if (status == AnalysisStatus.PENDING) {
					// For PENDING status, include both videos with PENDING analytics 
					// AND videos with no analytics at all
					predicates.add(
						builder.or(
							// Videos with PENDING analytics
							builder.equal(root.join(VideoAnalyzer_.ANALYTICS, jakarta.persistence.criteria.JoinType.LEFT).get(VideoAnalytics_.STATUS), status),
							// Videos with no analytics
							builder.isEmpty(root.get(VideoAnalyzer_.ANALYTICS))
						)
					);
				} else {
					// For other statuses, use the existing logic
					predicates.add(builder.equal(root.join(VideoAnalyzer_.ANALYTICS).get(VideoAnalytics_.STATUS), status));
				}
			} catch (IllegalArgumentException e) {
				// Invalid status value, ignore the filter
			}
		}
	}

	@Override
	public Predicate toPredicate(Root<VideoAnalyzer> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		String userRole = filter.getUserRole();

		if (RoleType.PLAYER.getRole().equalsIgnoreCase(userRole)) {
			addPlayerSpecificPredicates(root, builder, predicates);
		} else {
			addCoachSpecificPredicates(root, builder, predicates);
		}

		// Search
		addSearchLikePredicate(root, builder, predicates);

		addJoinInPredicate(root, builder, predicates, VideoAnalyzer_.COURSE, Course_.ID, filter.getProgramIds());
		addJoinInPredicate(root, builder, predicates, VideoAnalyzer_.PLAYER, UserProfile_.ID, filter.getPlayerIds());
		addJoinInPredicate(root, builder, predicates, VideoAnalyzer_.ACADEMY, Academy_.ID, filter.getAcademyIds());
		addEqualityPredicate(root, builder, predicates, VideoAnalyzer_.DELETED, !filter.isNotDeleted());
		addEqualityPredicate(root, builder, predicates, VideoAnalyzer_.IS_ANALYSED, filter.getAnalyzer().isAnalysed());

		// Analysis status filter
		addAnalysisStatusPredicate(root, builder, predicates);

		// Date range
		addDateTimeRangePredicate(root, builder, predicates);

		// Order by
		addOrderBy(root, query, builder);

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addJoinInPredicate(Root<VideoAnalyzer> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String id, List<String> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.join(field).get(id).in(values));
		}
	}

	// Helper method for IN predicates
	private static <T> void addInPredicate(Root<VideoAnalyzer> root, CriteriaBuilder builder,
			List<Predicate> predicates, String field, List<T> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.get(field).in(values));
		}
	}

}
