package com.playmotech.api.core.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.PaymentReminderType;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.PaymentReminderConfig;
import com.playmotech.api.core.dao_postgres.PaymentReminderConfig_;
import com.playmotech.api.core.dao_postgres.PaymentReminderEmailConfig;
import com.playmotech.api.core.dao_postgres.PaymentReminderMobileConfig;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PaymentReminderConfigSpecification implements Specification<PaymentReminderConfig> {
    private static final long serialVersionUID = 1L;

    private final transient GenericFilter filter;
    private final transient UserProfile currentUser;
    private final transient CoachAcademyMappingRepo coachAcademyMappingRepo;

    @Override
    public Predicate toPredicate(Root<PaymentReminderConfig> root, CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        // Filter by enabled status using paymentReminderConfig field
        if (filter.getPaymentReminderConfig() != null) {
            PaymentReminderConfig paymentReminderConfigFilter = filter.getPaymentReminderConfig();
            if (paymentReminderConfigFilter.getEnabled() != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("enabled"), paymentReminderConfigFilter.getEnabled()));
            }

            if (paymentReminderConfigFilter.getReminderTypeFilter() != null) {
                PaymentReminderType reminderType = PaymentReminderType
                        .valueOf(paymentReminderConfigFilter.getReminderTypeFilter().toUpperCase());
                predicates.add(criteriaBuilder.equal(root.get("reminderType"), reminderType));
            }
        }

        // Filter by academy ID (single academy)
        if (StringUtils.hasText(filter.getAcademyId())) {
            Join<PaymentReminderConfig, Academy> academyJoin = root.join("academies", JoinType.INNER);
            predicates.add(criteriaBuilder.equal(academyJoin.get("id"), filter.getAcademyId()));
        }

        // Filter by multiple academy IDs
        if (filter.getAcademyIds() != null && !filter.getAcademyIds().isEmpty()) {
            Join<PaymentReminderConfig, Academy> academyJoin = root.join("academies", JoinType.INNER);
            predicates.add(academyJoin.get("id").in(filter.getAcademyIds()));
        }

        String userRole = filter.getUserRole();

        log.debug("PaymentReminderConfigSpecification - current user role: {}, userId: {}", userRole,
                currentUser.getId());

        // Role-based access control logic
        if (userRole != null && !"SUPER_ADMIN".equals(userRole)) {
            List<CoachAcademyMapping> userCoachAcademyMapping = coachAcademyMappingRepo
                    .findByCoachUserProfile_Id(currentUser.getId());

            log.debug("coachAcademyMapping array size: {}", userCoachAcademyMapping.size());

            List<String> userAcademies = userCoachAcademyMapping.stream()
                    .map(each -> each.getAcademy() != null ? each.getAcademy().getId() : null)
                    .filter(Objects::nonNull).toList();

            log.debug("user academies array size: {}", userAcademies.size());

            // For any role other than SUPER_ADMIN, filter by their associated academies.
            if (!userAcademies.isEmpty()) {
                predicates.add(root.join(PaymentReminderConfig_.ACADEMIES, JoinType.LEFT).get("id").in(userAcademies));
            } else {
                // If user has no academies, return no results
                return criteriaBuilder.disjunction();
            }
        }

        // Search in title and message templates (now in related entities)
        if (StringUtils.hasText(filter.getSearch())) {
            String searchTerm = "%" + filter.getSearch().toLowerCase() + "%";
            
            // Left join with mobile config to search in mobile templates
            Join<PaymentReminderConfig, PaymentReminderMobileConfig> mobileConfigJoin = 
                    root.join("mobileConfig", JoinType.LEFT);
            
            // Left join with email config to search in email templates
            Join<PaymentReminderConfig, PaymentReminderEmailConfig> emailConfigJoin = 
                    root.join("emailConfig", JoinType.LEFT);
            
            predicates.add(
                    criteriaBuilder.or(
                            // Search in mobile config fields
                            criteriaBuilder.like(criteriaBuilder.lower(mobileConfigJoin.get("titleTemplate")), searchTerm),
                            criteriaBuilder.like(criteriaBuilder.lower(mobileConfigJoin.get("messageTemplate")), searchTerm),
                            // Search in email config fields
                            criteriaBuilder.like(criteriaBuilder.lower(emailConfigJoin.get("subjectTemplate")), searchTerm),
                            criteriaBuilder.like(criteriaBuilder.lower(emailConfigJoin.get("messageBody")), searchTerm)
                    ));
        }

        // Combine all predicates with AND
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
