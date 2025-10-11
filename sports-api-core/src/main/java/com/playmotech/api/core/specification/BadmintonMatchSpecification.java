package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.constants.MatchStatus;
import com.playmotech.api.core.dao_postgres.BadmintonMatch;
import com.playmotech.api.core.dao_postgres.BadmintonMatch_;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class BadmintonMatchSpecification implements Specification<BadmintonMatch> {

    private final transient GenericFilter filter;

    // Additional fields for specific filtering
    private final transient MatchStatus matchStatus;
    private final transient List<MatchStatus> matchStatuses;

    // Constructor for single match status
    public BadmintonMatchSpecification(GenericFilter filter, MatchStatus matchStatus) {
        this.filter = filter;
        this.matchStatus = matchStatus;
        this.matchStatuses = null;
    }

    // Constructor for multiple match statuses
    public BadmintonMatchSpecification(GenericFilter filter, List<MatchStatus> matchStatuses) {
        this.filter = filter;
        this.matchStatus = null;
        this.matchStatuses = matchStatuses;
    }

    // Constructor for no match status filtering (getAllMatches)
    public BadmintonMatchSpecification(GenericFilter filter) {
        this.filter = filter;
        this.matchStatus = null;
        this.matchStatuses = null;
    }

    private static boolean isValidOrderByColumn(String column) {
        return BadmintonMatch_.START_TIME.equals(column)
                || BadmintonMatch_.END_TIME.equals(column)
                || BadmintonMatch_.MATCH_STATUS.equals(column)
                || BadmintonMatch_.CREATED_AT_TIMESTAMP_UTC.equals(column)
                || BadmintonMatch_.UPDATED_AT_TIMESTAMP_UTC.equals(column)
                || BadmintonMatch_.SCHEDULED_START_TIME.equals(column)
                || "courtName".equals(column); // For court.courtName
    }

    private void addOrderBy(Root<BadmintonMatch> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
            String orderBy = filter.getOrderBy();
            Order order;

            if (isValidOrderByColumn(orderBy)) {
                if ("courtName".equals(orderBy)) {
                    // Special handling for court name
                    Join<Object, Object> courtJoin = root.join("court", JoinType.LEFT);
                    order = filter.isAscending() ? builder.asc(courtJoin.get("courtName"))
                            : builder.desc(courtJoin.get("courtName"));
                } else {
                    order = filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy));
                }
            } else {
                // Default sorting by created timestamp
                order = filter.isAscending() ? builder.asc(root.get(BadmintonMatch_.CREATED_AT_TIMESTAMP_UTC))
                        : builder.desc(root.get(BadmintonMatch_.CREATED_AT_TIMESTAMP_UTC));
            }

            query.orderBy(order);
        } else {
            // Default sorting when no orderBy is specified
            query.orderBy(builder.desc(root.get(BadmintonMatch_.CREATED_AT_TIMESTAMP_UTC)));
        }
    }

    private static <T> void addEqualityPredicate(Root<BadmintonMatch> root, CriteriaBuilder builder,
            List<Predicate> predicates, String field, T value) {
        if (value != null) {
            predicates.add(builder.equal(root.get(field), value));
        }
    }

    private static <T> void addInPredicate(Root<BadmintonMatch> root, CriteriaBuilder builder,
            List<Predicate> predicates, String field, List<T> values) {
        if (values != null && !values.isEmpty()) {
            predicates.add(root.get(field).in(values));
        }
    }

    private void addSearchLikePredicate(Root<BadmintonMatch> root, CriteriaBuilder builder,
            List<Predicate> predicates) {
        if (isStringNotNullAndBlank.test(filter.getSearch()) && filter.getSearch().length() > 2) {
            String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";

            List<Predicate> searchPredicates = new ArrayList<>();

            // Search in court name
            Join<Object, Object> courtJoin = root.join("court", JoinType.LEFT);
            searchPredicates.add(builder.like(builder.lower(courtJoin.get("courtName")), search));

            // Search in single players display names
            Join<Object, Object> singlePlayersJoin = root.join("singlePlayers", JoinType.LEFT);
            Join<Object, Object> singlePlayerProfileJoin = singlePlayersJoin.join("playerUserProfile", JoinType.LEFT);
            searchPredicates.add(builder.like(builder.lower(singlePlayerProfileJoin.get("displayName")), search));

            // Search in single players guest names
            searchPredicates.add(builder.like(builder.lower(singlePlayersJoin.get("guestName")), search));

            // Search in team players display names
            Join<Object, Object> teamPlayersJoin = root.join("badmintonMatchTeamPlayers", JoinType.LEFT);
            Join<Object, Object> teamPlayerProfileJoin = teamPlayersJoin.join("playerUserProfile", JoinType.LEFT);
            searchPredicates.add(builder.like(builder.lower(teamPlayerProfileJoin.get("displayName")), search));

            // Search in team players guest names
            searchPredicates.add(builder.like(builder.lower(teamPlayersJoin.get("guestName")), search));

            // Search in team names
            Join<Object, Object> teamsJoin = root.join("teams", JoinType.LEFT);
            Join<Object, Object> teamJoin = teamsJoin.join("team", JoinType.LEFT);
            searchPredicates.add(builder.like(builder.lower(teamJoin.get("teamName")), search));

            predicates.add(builder.or(searchPredicates.toArray(new Predicate[0])));
        }
    }

    private void addActiveFilter(Root<BadmintonMatch> root, CriteriaBuilder builder, List<Predicate> predicates) {
        // Always filter out inactive matches
        predicates.add(builder.isFalse(root.get("inactive")));
    }

    private void addAcademyFilter(Root<BadmintonMatch> root, CriteriaBuilder builder, List<Predicate> predicates) {
        if (isStringNotNullAndBlank.test(filter.getAcademyId())) {
            predicates.add(builder.equal(root.get("academy").get("id"), filter.getAcademyId()));
        }
    }

    private void addTournamentFilter(Root<BadmintonMatch> root, CriteriaBuilder builder, List<Predicate> predicates) {
        if (isStringNotNullAndBlank.test(filter.getTournamentId())) {
            predicates.add(builder.equal(root.get("tournament").get("id"), filter.getTournamentId()));
        }
    }

    private void addMatchStatusFilter(Root<BadmintonMatch> root, CriteriaBuilder builder, List<Predicate> predicates) {
        if (matchStatus != null) {
            addEqualityPredicate(root, builder, predicates, "matchStatus", matchStatus);
        } else if (!CollectionUtils.isEmpty(matchStatuses)) {
            addInPredicate(root, builder, predicates, "matchStatus", matchStatuses);
        }
    }

    @Override
    public Predicate toPredicate(Root<BadmintonMatch> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();

        // Always filter active matches
        addActiveFilter(root, builder, predicates);

        // Add academy filter
        addAcademyFilter(root, builder, predicates);

        // Add tournament filter
        addTournamentFilter(root, builder, predicates);

        // Add match status filter
        addMatchStatusFilter(root, builder, predicates);

        // Add search filter
        addSearchLikePredicate(root, builder, predicates);

        // Add sorting
        addOrderBy(root, query, builder);

        return builder.and(predicates.toArray(new Predicate[0]));
    }
}