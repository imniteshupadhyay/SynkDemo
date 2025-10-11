package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class TeamMatchPlayerDto {
    private String teamId;
    private List<String> players;
    private List<String> guestPlayers;
}
