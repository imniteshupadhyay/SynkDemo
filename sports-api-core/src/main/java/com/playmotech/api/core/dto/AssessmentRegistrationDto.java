package com.playmotech.api.core.dto;

import java.time.LocalDate;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration.PlayerStatus;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration.RegistrationPaymentStatus;

import lombok.Data;

@Data
public class AssessmentRegistrationDto {

    private String registrationId;
    private String playerId; // modelMapper needed
    private UserProfileMinDto playerProfile; // modelMapper needed
    private String academyId; // modelMapper needed
    private AcademyDto academy; // modelMapper needed
    private LocalDate registrationDate;
    private String assessmentId; // modelMapper needed
    private String registrationNumber;
    private Gender gender;
    private PlayerStatus playerStatus;
    private Double paymentAmount;
    private RegistrationPaymentStatus paymentStatus;
}
