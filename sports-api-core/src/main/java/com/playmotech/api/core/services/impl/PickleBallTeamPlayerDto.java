package com.playmotech.api.core.services.impl;

import com.playmotech.api.core.dto.UserProfileMinDto;

import lombok.Data;

@Data
public class PickleBallTeamPlayerDto {
    private String playerUserId;
    private String guestPlayerName;
    private UserProfileMinDto userProfile;
}
