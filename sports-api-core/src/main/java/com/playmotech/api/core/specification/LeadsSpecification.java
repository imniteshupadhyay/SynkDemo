package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.Academy_;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.LeadSource_;
import com.playmotech.api.core.dao_postgres.Leads;
import com.playmotech.api.core.dao_postgres.Leads_;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.UserProfile_;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.views.TraineeView_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class LeadsSpecification implements Specification<Leads> {
	private static final long serialVersionUID = 1L;

	private final transient GenericFilter filter;
	private final transient UserProfile currentUser;
	private final transient CoachAcademyMappingRepo coachAcademyMappingRepo;

	private static <T> void addEqualityPredicate(Root<Leads> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, String string) {
		if (string != null) {
			predicates.add(builder.equal(root.get(field), string));
		}
	}

	private void addJoinInPredicate(Root<Leads> root, CriteriaBuilder builder, List<Predicate> predicates, String field,
			String id, List<String> values) {
		if (values != null && !values.isEmpty()) {
			predicates.add(root.join(field).get(id).in(values));
		}
	}

	private static <T> void addInPredicate(Root<Leads> root, CriteriaBuilder builder, List<Predicate> predicates,
			String field, List<String> list) {
		if (list != null && !list.isEmpty()) {
			predicates.add(root.get(field).in(list));
		}
	}

	@Override
	public Predicate toPredicate(Root<Leads> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
		List<Predicate> predicates = new ArrayList<>();

		// Search filter (applies to multiple fields)
		if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
			String searchPattern = "%" + filter.getSearch().toLowerCase().trim() + "%";
			predicates.add(criteriaBuilder.or(
					// Basic contact information
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.NAME)), searchPattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.PHONE_NUMBER)), searchPattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.EMAIL_ID)), searchPattern),

					// Address information
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.ADDRESS_LINE1)), searchPattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.ADDRESS_LINE2)), searchPattern),
					// ID search
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.ID)), searchPattern),

					// Related entities
					criteriaBuilder.like(
							criteriaBuilder.lower(root.join(Leads_.LEAD_SOURCE, JoinType.LEFT).get(LeadSource_.NAME)),
							searchPattern),
					criteriaBuilder.like(
							criteriaBuilder.lower(root.join(Leads_.ASSIGNED_ACADEMY, JoinType.LEFT).get(Academy_.NAME)),
							searchPattern),
					criteriaBuilder.like(
							criteriaBuilder.lower(
									root.join(Leads_.ASSIGNED_COACH, JoinType.LEFT).get(UserProfile_.DISPLAY_NAME)),
							searchPattern),
					criteriaBuilder.like(
							criteriaBuilder.lower(
									root.join(Leads_.LEAD_CREATED_BY, JoinType.LEFT).get(UserProfile_.DISPLAY_NAME)),
							searchPattern),

					// Status as string (for partial status matches)
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.LEAD_STATUS).as(String.class)),
							searchPattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.GENDER).as(String.class)),
							searchPattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.SPORTS).as(String.class)),
							searchPattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get(Leads_.AGE_CATEGORY).as(String.class)),
							searchPattern)));
		}

		// Filter by sports list
		if (filter.getSports() != null && !filter.getSports().isEmpty()) {
			List<Predicate> sportPredicates = new ArrayList<>();
			for (String sport : filter.getSports()) {
				try {
					Sports sportEnum = Sports.valueOf(sport);
					sportPredicates.add(criteriaBuilder.equal(root.get(Leads_.SPORTS), sportEnum));
				} catch (Exception e) {
					log.warn("Sport value: {}", sport);
					e.printStackTrace();
				}
			}

			if (!sportPredicates.isEmpty()) {
				predicates.add(criteriaBuilder.or(sportPredicates.toArray(new Predicate[0])));
			}
		}

		// Filter by single sport
		if (filter.getSport() != null && !filter.getSport().isEmpty()) {
			try {
				Sports sportEnum = Sports.valueOf(filter.getSport());
				predicates.add(criteriaBuilder.equal(root.get(Leads_.SPORTS), sportEnum));
			} catch (Exception e) {
				e.printStackTrace();
				log.warn("Invalid sport value: {}", filter.getSport());
			}
		}

		// Filter by age categories
		if (filter.getAgeCategory() != null && !filter.getAgeCategory().isEmpty()) {
			List<Predicate> ageCategoryPredicates = new ArrayList<>();

			for (String category : filter.getAgeCategory()) {
				try {
					AgeCategory ageCategoryEnum = AgeCategory.valueOf(category);
					ageCategoryPredicates.add(criteriaBuilder.equal(root.get(Leads_.AGE_CATEGORY), ageCategoryEnum));
				} catch (Exception e) {
					e.printStackTrace();
					log.warn("Invalid age category value: {}", category);
				}
			}

			if (!ageCategoryPredicates.isEmpty()) {
				predicates.add(criteriaBuilder.or(ageCategoryPredicates.toArray(new Predicate[0])));
			}
		}

		// Filter by lead source
		if (!filter.getLeads().getLeadSourceIds().isEmpty() && filter.getLeads().getLeadSourceIds() != null) {
			predicates.add(root.join(Leads_.LEAD_SOURCE, JoinType.LEFT).get(LeadSource_.ID)
					.in(filter.getLeads().getLeadSourceIds()));
		}

		// Filter by academy IDs
		if (filter.getAcademyIds() != null && !filter.getAcademyIds().isEmpty()) {
			predicates
					.add(root.join(Leads_.ASSIGNED_ACADEMY, JoinType.LEFT).get(Academy_.ID).in(filter.getAcademyIds()));
		}

		// Filter by single academy ID
		if (filter.getAcademyId() != null && !filter.getAcademyId().isEmpty()) {
			predicates.add(criteriaBuilder.equal(root.join(Leads_.ASSIGNED_ACADEMY, JoinType.LEFT).get(Academy_.ID),
					filter.getAcademyId()));
		}

		// Role-based access control
		// Fetch academy on the basis of origin and check if that academy & user id is
		// present in the coach_academy_mapping
		String userRole = filter.getUserRole();

		log.debug("LeadsSpecification - current user role: {}", userRole);
		List<CoachAcademyMapping> userCoachAcademyMapping = coachAcademyMappingRepo
				.findByCoachUserProfile_Id(currentUser.getId());
		log.debug("coachAcademyMapping array size: {}", userCoachAcademyMapping.size());

		List<String> academies = userCoachAcademyMapping.stream()
				.map(each -> each.getAcademy() != null ? each.getAcademy().getId() : null).peek(log::debug)
				.filter(Objects::nonNull).toList();
		log.debug("academies array size: {}", academies.size());
		if (!"SUPER_ADMIN".equals(userRole)) {
			if ("COACH".equals(userRole)) {
				// Coaches should not see any leads
				return criteriaBuilder.disjunction();
			} else {
				// User can see leads from their academies OR leads they personally created
				Predicate academyPredicate = root.join(Leads_.ASSIGNED_ACADEMY, JoinType.LEFT).get(Academy_.ID)
						.in(academies);

				Predicate createdByUser = criteriaBuilder.equal(
						root.join(Leads_.LEAD_CREATED_BY, JoinType.LEFT).get(UserProfile_.ID), currentUser.getId());

				predicates.add(criteriaBuilder.or(academyPredicate, createdByUser));
			}
		}

		// Date range filters
		if (filter.getStartDate() != null) {
			predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Leads_.CREATED_ON), filter.getStartDate()));
		}
		if (filter.getEndDate() != null) {
			predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Leads_.CREATED_ON), filter.getEndDate()));
		}

		// Sorting
//		if (filter.getOrderBy() != null && !filter.getOrderBy().isEmpty()) {
//			if (filter.isAscending()) {
//				query.orderBy(criteriaBuilder.asc(root.get(filter.getOrderBy())));
//			} else {
//				query.orderBy(criteriaBuilder.desc(root.get(filter.getOrderBy())));
//			}
//		}

		addOrderBy(root, query, criteriaBuilder);

		if (filter.getLeads().getStatus() != null && !filter.getLeads().getStatus().isEmpty()) {
			addInPredicate(root, criteriaBuilder, predicates, Leads_.LEAD_STATUS, filter.getLeads().getStatus());
		}

		if (filter.isNotDeleted()) {
			predicates.add(criteriaBuilder.equal(root.get(Leads_.INACTIVE), !filter.isNotDeleted()));
		}

		return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
	}

	private void addOrderBy(Root<Leads> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
			String orderBy = filter.getOrderBy();
			Path<?> path = switch (orderBy) {
			case "name" -> root.get(Leads_.NAME);
			case "emailId" -> root.get(Leads_.EMAIL_ID);
			case "phoneNumber" -> root.get(Leads_.PHONE_NUMBER);
			case "sports" -> root.get(Leads_.SPORTS);
			case "ageCategory" -> root.get(Leads_.AGE_CATEGORY);
			case "leadSource" -> root.get(Leads_.LEAD_SOURCE);
			case "leadStatus" -> root.get(Leads_.LEAD_STATUS);
			case "createdOn" -> root.get(Leads_.CREATED_ON);
			default -> root.get(TraineeView_.ID);
			};
			query.orderBy(filter.isAscending() ? builder.asc(path) : builder.desc(path));
		}
	}

}