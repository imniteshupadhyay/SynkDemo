package com.playmotech.api.core.specification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.dao_postgres.CoachPointsBalance;
import com.playmotech.api.core.dao_postgres.CoachPointsBalance_;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dao_postgres.UserProfile_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CoachPointBalanceSpecification implements Specification<CoachPointsBalance> {
    /** Filter by organisation id */
    private static Specification<CoachPointsBalance> byOrganisationId(String organisationId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(organisationId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(CoachPointsBalance_.ORGANISATION).get("id"), organisationId);
        };
    }

    /** Filter by academy id */
    private static Specification<CoachPointsBalance> byAcademyId(String academyId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(academyId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(CoachPointsBalance_.ACADEMY).get("id"), academyId);
        };
    }

    /**
     * Filter by academy specific flag
     */
    public static Specification<CoachPointsBalance> byAcademySpecific(boolean isAcademySpecific) {
        return (root, query, criteriaBuilder) -> criteriaBuilder
                .equal(root.get(CoachPointsBalance_.IS_ACADEMY_SPECIFIC), isAcademySpecific);
    }

    /**
     * Filter by time period - entries updated since startDate
     */
    public static Specification<CoachPointsBalance> byTimePeriod(LocalDateTime startDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(CoachPointsBalance_.LAST_UPDATED_AT), startDate);
        };
    }

    /**
     * Search by coach name (displayName, username)
     */
    public static Specification<CoachPointsBalance> byCoachNameContaining(String searchQuery) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(searchQuery)) {
                return criteriaBuilder.conjunction();
            }

            Join<CoachPointsBalance, UserProfile> coachJoin = root.join(CoachPointsBalance_.COACH);

            String likePattern = "%" + searchQuery.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(coachJoin.get(UserProfile_.DISPLAY_NAME)), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(coachJoin.get(UserProfile_.USERNAME)), likePattern));
        };
    }

    /**
     * Get a coach's specific entry
     */
    public static Specification<CoachPointsBalance> byCoachId(String coachId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(coachId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(CoachPointsBalance_.COACH).get("id"), coachId);
        };
    }

    /**
     * Combine filters for organization leaderboard
     */
    public static Specification<CoachPointsBalance> organisationLeaderboardSpec(
            String organisationId, LocalDateTime startDate, String searchQuery) {

        Specification<CoachPointsBalance> spec = Specification
                .where(byOrganisationId(organisationId))
                .and(byTimePeriod(startDate));

        if (StringUtils.hasText(searchQuery)) {
            spec = spec.and(byCoachNameContaining(searchQuery));
        }

        return spec;
    }

    /**
     * Combine filters for academy leaderboard
     */
    public static Specification<CoachPointsBalance> academyLeaderboardSpec(
            String academyId, LocalDateTime startDate, String searchQuery) {

        Specification<CoachPointsBalance> spec = Specification
                .where(byAcademyId(academyId))
                .and(byTimePeriod(startDate));

        if (StringUtils.hasText(searchQuery)) {
            spec = spec.and(byCoachNameContaining(searchQuery));
        }

        return spec;
    }

    public static Specification<CoachPointsBalance> globalLeaderboardSpec(LocalDateTime startDate, String searchQuery) {
        // Start with time period filtering
        Specification<CoachPointsBalance> spec = Specification.where(byTimePeriod(startDate));
        if (StringUtils.hasText(searchQuery)) {
            spec = spec.and(byCoachNameContaining(searchQuery));
        }
        return spec;
    }

    @Override
    public Predicate toPredicate(Root<CoachPointsBalance> root, CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder) {
        return null;
    }
}
