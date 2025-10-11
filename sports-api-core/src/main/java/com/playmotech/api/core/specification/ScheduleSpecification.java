package com.playmotech.api.core.specification;

import static com.playmotech.api.core.utils.SpecificationUtil.isStringNotNullAndBlank;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.playmotech.api.core.dao_postgres.NewSchedule;
import com.playmotech.api.core.dao_postgres.NewSchedule_;
import com.playmotech.api.core.dao_postgres.Organisation_;
import com.playmotech.api.core.utils.EnumUtil.RoleType;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("serial")
public class ScheduleSpecification implements Specification<NewSchedule> {

    private final transient GenericFilter filter;

    private static boolean isValidOrderByColumn(String column) {
        return NewSchedule_.NAME.equals(column) || NewSchedule_.START_DATE.equals(column)
                || NewSchedule_.END_DATE.equals(column) || NewSchedule_.INSERTED_ON.equals(column);
    }

    private static <T> void addEqualityPredicate(Root<NewSchedule> root, CriteriaBuilder builder,
            List<Predicate> predicates, String field, T value) {
        if (value != null) {
            predicates.add(builder.equal(root.get(field), value));
        }
    }

    private void addOrderBy(Root<NewSchedule> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        if (isStringNotNullAndBlank.test(filter.getOrderBy())) {
            String orderBy = filter.getOrderBy();
            if (isValidOrderByColumn(orderBy)) {
                query.orderBy(filter.isAscending() ? builder.asc(root.get(orderBy)) : builder.desc(root.get(orderBy)));
            } else {
                query.orderBy(filter.isAscending() ? builder.asc(root.get(NewSchedule_.INSERTED_ON))
                        : builder.desc(root.get(NewSchedule_.INSERTED_ON)));
            }
        }
    }

    private void addSearchLikePredicate(Root<NewSchedule> root, CriteriaBuilder builder, List<Predicate> predicates) {
        if (isStringNotNullAndBlank.test(filter.getSearch())) {
            String search = "%" + filter.getSearch().toLowerCase(Locale.ENGLISH) + "%";
            predicates.add(builder.or(builder.like(builder.lower(root.get(NewSchedule_.NAME)), search),
                    builder.like(builder.lower(root.get(NewSchedule_.DESCRIPTION)), search)));
        }
    }

    private void addDateRangePredicate(Root<NewSchedule> root, CriteriaBuilder builder,
            List<Predicate> predicates) {
        if (filter.getStartDate() != null) {
            LocalDate start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            predicates.add(builder.greaterThanOrEqualTo(root.get(NewSchedule_.START_DATE), start));
        }

        if (filter.getEndDate() != null) {
            LocalDate end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            predicates.add(builder.lessThanOrEqualTo(root.get(NewSchedule_.END_DATE), end));
        }
    }
    
    private void addTypeFilter(Root<NewSchedule> root, CriteriaBuilder builder, List<Predicate> predicates) {
//        if (filter.getScheduleType() != null) {
//            predicates.add(builder.equal(root.get(NewSchedule_.TYPE), ScheduleType.valueOf(filter.getScheduleType())));
//        }
    }

    @Override
    public Predicate toPredicate(Root<NewSchedule> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();

        String userRole = filter.getUserRole();
		if (!RoleType.SUPER_ADMIN.name().equalsIgnoreCase(userRole)) {
			addJoinInPredicate(root, builder, predicates, NewSchedule_.ORGANISATION, Organisation_.DOMAIN_URL, filter.getDomainUrl());
		}

        // Search
        addSearchLikePredicate(root, builder, predicates);

        // Type filter
        addTypeFilter(root, builder, predicates);

        
        // Deleted filter
        addEqualityPredicate(root, builder, predicates, NewSchedule_.DELETED, !filter.isNotDeleted());

        // Date range
        addDateRangePredicate(root, builder, predicates);

        // Order by
        addOrderBy(root, query, builder);

        return builder.and(predicates.toArray(new Predicate[0]));
    }
    
    private void addJoinInPredicate(Root<NewSchedule> root, CriteriaBuilder builder, List<Predicate> predicates,
            String field, String id, String values) {
        if (values != null) {
            predicates.add(root.join(field).get(id).in(values));
        }
    }

    private void addJoinInPredicate(Root<NewSchedule> root, CriteriaBuilder builder, List<Predicate> predicates,
            String field, String id, List<Long> values) {
        if (values != null && !values.isEmpty()) {
            predicates.add(root.join(field).get(id).in(values));
        }
    }
}