package com.playmotech.api.core.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping_;
import com.playmotech.api.core.dao_postgres.Roles_;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.UserProfile_;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserProfileSpecification implements Specification<UserProfile> {
	private static final long serialVersionUID = -5744857265526506942L;
	private final transient GenericFilter filter;
	private final transient UserProfileRepo userProfileRepository;

	@Override
	public Predicate toPredicate(Root<UserProfile> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		// Search filter
		addSearchLikePredicate(root, builder, predicates);

		// Determine user role based on logged-in user
		String userRole = filter.getUserRole();

		if ("COACHES".equals(userRole) || "COACH".equals(userRole)) {
			addCoachSpecifications(root, builder, predicates);
		} else if ("ADMIN".equals(userRole)) {
			addAdminSpecifications(root, builder, query, predicates);
		} else if ("ACADEMY_OWNER".equals(userRole)) {
			addAcademyOwnerSpecifications(root, builder, query, predicates);
		} else if ("SUPER_ADMIN".equals(userRole)) {
			// No additional predicates for SUPER_ADMIN as they can see all users
		}

		return builder.and(predicates.toArray(new Predicate[0]));
	}

	private void addCoachSpecifications(Root<UserProfile> root, CriteriaBuilder builder, List<Predicate> predicates) {
		// predicates.add(builder.equal(root.get(CoachAcademyMapping_.COACH_USER_PROFILE).get(UserProfile_.ID),
		// filter.getUserId()));
		predicates.add(builder.equal(root.join(CoachAcademyMapping_.COACH_USER_PROFILE).get(UserProfile_.ID),
				filter.getUserId()));
	}

	private void addAdminSpecifications(Root<UserProfile> root, CriteriaBuilder builder, CriteriaQuery<?> query,
			List<Predicate> predicates) {
		// Admin can only see users under the academy they belong to
		if (filter.getUserId() != null) {
			// First get the UserProfile of the admin
			Optional<UserProfile> adminUserOptional = userProfileRepository
					.findByIdAndInactiveIsFalse(filter.getUserId());
			if (adminUserOptional.isPresent()) {
				// Find the academy this admin is associated with
				// This will require a join or subquery to find the academy this admin belongs
				// to
				// For this example, we'll use a subquery
				Subquery<String> adminAcademySubquery = query.subquery(String.class);
				Root<Academy> academyRoot = adminAcademySubquery.from(Academy.class);
				adminAcademySubquery.select(academyRoot.get("id"))
						.where(builder.equal(academyRoot.get("managerUserId"), filter.getUserId()));

				// Now find all users associated with this academy
				// This will require a join or subquery based on your actual data model
				// For this example, we'll use CoachAcademyMapping
				Subquery<String> usersInAcademySubquery = query.subquery(String.class);
				Root<CoachAcademyMapping> mappingRoot = usersInAcademySubquery.from(CoachAcademyMapping.class);
				usersInAcademySubquery.select(mappingRoot.get("coachUserProfile").get("id"))
						.where(mappingRoot.get("academy").get("id").in(adminAcademySubquery));

				predicates.add(root.get("id").in(usersInAcademySubquery));
			}
		}
	}

	private void addAcademyOwnerSpecifications(Root<UserProfile> root, CriteriaBuilder builder, CriteriaQuery<?> query,
			List<Predicate> predicates) {
		// Academy owner can see users under all academies they own
		if (filter.getUserId() != null) {
			// First, get all academies owned by this user (assuming academy owner is
			// identified by managerUserId)
			Subquery<String> ownerAcademySubquery = query.subquery(String.class);
			Root<Academy> academyRoot = ownerAcademySubquery.from(Academy.class);
			ownerAcademySubquery.select(academyRoot.get("id"))
					.where(builder.equal(academyRoot.get("managerUserId"), filter.getUserId()));

			// Then, get all users associated with these academies
			// This will require a join or subquery based on your actual data model
			// For this example, we'll use CoachAcademyMapping
			Subquery<String> usersInAcademySubquery = query.subquery(String.class);
			Root<CoachAcademyMapping> mappingRoot = usersInAcademySubquery.from(CoachAcademyMapping.class);
			usersInAcademySubquery.select(mappingRoot.get("coachUserProfile").get("id"))
					.where(mappingRoot.get("academy").get("id").in(ownerAcademySubquery));

			predicates.add(root.get("id").in(usersInAcademySubquery));
		}
	}

	private String getUserRole() {
		if (filter.getUserId() != null) {
			Optional<UserProfile> userOptional = userProfileRepository.findByIdAndInactiveIsFalse(filter.getUserId());
			return userOptional.map(user -> user.getRole().name()).orElse(null);
		}
		return null;
	}

	private void addSearchLikePredicate(Root<UserProfile> root, CriteriaBuilder builder, List<Predicate> predicates) {
		if (StringUtils.hasText(filter.getSearch())) {
			String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";
			predicates.add(builder.or(builder.like(builder.lower(root.get(UserProfile_.DISPLAY_NAME)), search),
					builder.like(builder.lower(root.get(UserProfile_.EMAIL_ID)), search),
					builder.like(builder.lower(root.get(UserProfile_.PHONE_NUMBER)), search),
					builder.like(builder.lower(root.join(UserProfile_.ROLE, JoinType.LEFT).get(Roles_.ROLE_NAME)),
							search),
					builder.like(builder.lower(root.get(UserProfile_.USER_TYPE)), search)));
		}
	}
}
