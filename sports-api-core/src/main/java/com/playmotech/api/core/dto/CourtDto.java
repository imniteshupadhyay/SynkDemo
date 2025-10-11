package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.Sports;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class CourtDto {
    private String id;

    @NotEmpty(message = "Court name cannot be empty")
    private String courtName;

    private String courtImage;

    private Sports sports;
}
