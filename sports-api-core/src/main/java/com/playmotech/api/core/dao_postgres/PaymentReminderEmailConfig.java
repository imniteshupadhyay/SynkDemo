package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity for email notification templates linked to a payment reminder configuration.
 */
@Entity
@Table(name = "payment_reminder_email_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PaymentReminderEmailConfig {

    /**
     * Primary key equals parent PaymentReminderConfig id (shared PK).
     */
    @Id
    private Long id;

    /**
     * Back-reference to parent configuration.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "config_id")
    private PaymentReminderConfig config;

    /**
     * Subject template with placeholders {amount}, {days}
     */
    @Column(name = "subject_template", nullable = false)
    private String subjectTemplate;

    /**
     * Full email body content (HTML) with placeholders {amount}, {days}, etc.
     */
    @Column(name = "message_body", columnDefinition = "TEXT", nullable = false)
    private String messageBody;
}
