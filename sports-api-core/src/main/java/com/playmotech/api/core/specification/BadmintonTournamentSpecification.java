package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.TournamentStatus;
import com.playmotech.api.core.dao_postgres.Academy_;
import com.playmotech.api.core.dao_postgres.BadmintonTournament;
import com.playmotech.api.core.dao_postgres.BadmintonTournament_;
import com.playmotech.api.core.utils.DateTimeUtils;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@SuppressWarnings("serial")
@Slf4j
public class BadmintonTournamentSpecification implements Specification<BadmintonTournament> {

    private final transient GenericFilter filter;
    private final transient TournamentStatus tournamentStatus;

    private static boolean isValidOrderByColumn(String column) {
        return BadmintonTournament_.START_DATE.equals(column)
                || BadmintonTournament_.END_DATE.equals(column)
                || BadmintonTournament_.CREATED_ON.equals(column);
    }

    private void addActiveFilter(Root<BadmintonTournament> root, CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates) {
        predicates.add(criteriaBuilder.equal(root.get(BadmintonTournament_.INACTIVE), false));
    }

    private void addAcademyFilter(Root<BadmintonTournament> root, CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates) {
        if (StringUtils.hasText(filter.getAcademyId())) {
            predicates.add(criteriaBuilder.equal(root.get(BadmintonTournament_.ACADEMY).get(Academy_.ID),
                    filter.getAcademyId()));
        }
    }

    private void addSearchLikePredicate(Root<BadmintonTournament> root, CriteriaBuilder builder,
            List<Predicate> predicates) {

        // Search text filter (if provided and length > 2)
        if (StringUtils.hasText(filter.getSearch()) && filter.getSearch().length() > 2) {
            predicates.add(builder.like(
                    builder.lower(root.get(BadmintonTournament_.NAME)),
                    "%" + filter.getSearch().toLowerCase() + "%"));
        }
    }

    private void addOrderBy(Root<BadmintonTournament> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
            String orderBy = filter.getOrderBy();
            Order order;

            if (isValidOrderByColumn(orderBy)) {
                order = filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy));
            } else {
                // Default sorting by createdOn
                order = filter.isAscending() ? builder.asc(root.get(BadmintonTournament_.CREATED_ON))
                        : builder.desc(root.get(BadmintonTournament_.CREATED_ON));
            }

            query.orderBy(order);
        } else {
            // Default sorting when no orderBy is specified
            query.orderBy(builder.desc(root.get(BadmintonTournament_.CREATED_ON)));
        }
    }

    private void addTournamentStatusPredicate(Root<BadmintonTournament> root, CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates) {
        if (tournamentStatus != null) {
            LocalDate today;
            try {
                today = LocalDate.parse(DateTimeUtils.getTodayDate());
            } catch (DateTimeParseException e) {
                log.error("[BadmintonTournamentSpecification - DateTimeParseException]: {}", e.getMessage());
                today = LocalDate.now();
            }

            // Convert LocalDate to String (YYYY-MM-DD)
            String todayStr = today.toString();

            switch (tournamentStatus) {
                case LIVE:
                    predicates.add(
                            criteriaBuilder.lessThanOrEqualTo(root.get(BadmintonTournament_.START_DATE), todayStr));
                    predicates
                            .add(criteriaBuilder.greaterThanOrEqualTo(root.get(BadmintonTournament_.END_DATE),
                                    todayStr));
                    break;
                case UPCOMING:
                    predicates.add(criteriaBuilder.greaterThan(root.get(BadmintonTournament_.START_DATE), todayStr));
                    break;
                case COMPLETED:
                    predicates.add(criteriaBuilder.lessThan(root.get(BadmintonTournament_.END_DATE), todayStr));
                    break;
                default:
                    break;
                // For null or other statuses, no additional date filtering is applied
            }
        }
    }

    @Override
    public Predicate toPredicate(Root<BadmintonTournament> root, CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder) {

        List<Predicate> predicates = new ArrayList<>();

        // Always filter active matches
        addActiveFilter(root, criteriaBuilder, predicates);

        // Academy filter
        addAcademyFilter(root, criteriaBuilder, predicates);

        // Tournament status filter
        addTournamentStatusPredicate(root, criteriaBuilder, predicates);

        // Add search predicate (name)
        addSearchLikePredicate(root, criteriaBuilder, predicates);

        // Add order
        addOrderBy(root, query, criteriaBuilder);

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}