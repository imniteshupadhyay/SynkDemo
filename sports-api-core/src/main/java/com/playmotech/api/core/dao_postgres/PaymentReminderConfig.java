package com.playmotech.api.core.dao_postgres;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.playmotech.api.core.constants.PaymentReminderType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity for storing payment reminder configurations
 * Each configuration can apply to multiple academies
 */
@Entity
@Table(name = "payment_reminder_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PaymentReminderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Academies this configuration applies to
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "payment_reminder_config_academies", joinColumns = @JoinColumn(name = "config_id"), inverseJoinColumns = @JoinColumn(name = "academy_id"))
    private Set<Academy> academies = new HashSet<>();

    /**
     * Type of reminder (UPCOMING or OVERDUE)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false)
    private PaymentReminderType reminderType;

    /**
     * Whether push (mobile) notifications should be sent for this reminder.
     */
    @Column(name = "send_push_notification", nullable = false)
    private Boolean sendPushNotification = Boolean.TRUE;

    /**
     * Whether email notifications should be sent for this reminder.
     */
    @Column(name = "send_email_notification", nullable = false)
    private Boolean sendEmailNotification = Boolean.FALSE;

    /**
     * Mobile (push) notification template configuration.
     */
    @OneToOne(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private PaymentReminderMobileConfig mobileConfig;

    /**
     * Email notification template configuration.
     */
    @OneToOne(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private PaymentReminderEmailConfig emailConfig;

    /**
     * Time in UTC when notification should be sent
     */
    @Column(name = "notification_time", nullable = false)
    private LocalTime notificationTime;

    /**
     * Frequency in days for reminders (threshold for upcoming, frequency for
     * overdue)
     */
    @Column(name = "frequency_days", nullable = false)
    private Integer frequencyDays;

    /**
     * Whether this configuration is enabled
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "created_at")
    @CreationTimestamp
    private String createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private String updatedAt;

    @Transient
    @JsonIgnore
    private String reminderTypeFilter;
}
