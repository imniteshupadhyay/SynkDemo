package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coach_point_transaction_error")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoachPointTransactionError {
    @Id
    private String id;

    @Column(name = "original_transaction_id")
    private String originalTransactionId;

    @Column(name = "error_message")
    private String errorMessage;

    @Lob
    private String failedEntityData; // JSON representation

    @Column(name = "error_time")
    private LocalDateTime errorTime;
}
