package com.playmotech.api.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.dto.BadmintonMatchDetailDto;
import com.playmotech.api.core.dto.BadmintonMatchDetailRequestDto;
import com.playmotech.api.core.dto.BadmintonMatchDto;
import com.playmotech.api.core.dto.BadmintonMatchRoundDetailsDto;
import com.playmotech.api.core.dto.BadmintonMatchRoundDetailsRequestDto;
import com.playmotech.api.core.dto.BadmintonPlayerScoreDto;
import com.playmotech.api.core.dto.BadmintonScoreRequestDto;
import com.playmotech.api.core.dto.BadmintonTournamentDto;
import com.playmotech.api.core.dto.CreateBadmintonMatchDto;
import com.playmotech.api.core.dto.CreatePickleballMatchDto;
import com.playmotech.api.core.dto.PickleballMatchDetailDto;
import com.playmotech.api.core.dto.PickleballMatchDetailRequestDto;
import com.playmotech.api.core.dto.PickleballMatchDto;
import com.playmotech.api.core.dto.PickleballMatchRoundDetailsDto;
import com.playmotech.api.core.dto.PickleballMatchRoundDetailsRequestDto;
import com.playmotech.api.core.dto.PickleballPlayerScoreDto;
import com.playmotech.api.core.dto.PickleballPlayerScoreRequestDto;
import com.playmotech.api.core.dto.PickleballScoreRequestDto;
import com.playmotech.api.core.dto.PickleballTeamDto;
import com.playmotech.api.core.dto.PickleballTournamentDto;
import com.playmotech.api.core.dto.TeamDto;
import com.playmotech.api.core.dto.TeamPlayerDto;
import com.playmotech.api.core.dto.UpdateBadmintonMatchDto;
import com.playmotech.api.core.dto.UpdateBadmintonTournamentDto;
import com.playmotech.api.core.dto.UpdatePickleballMatchDto;
import com.playmotech.api.core.dto.UpdatePickleballTournamentDto;
import com.playmotech.api.core.services.impl.PickleBallTeamPlayerDto;

@Service
public class EntityToDtoUtils {
    private final ModelMapper modelMapper = new ModelMapper();

    // Generic mapping utility methods
    private <T, R> List<R> mapList(List<T> source, Function<T, R> mapper) {
        return source != null
                ? source.stream().filter(Objects::nonNull).map(mapper).toList()
                : new ArrayList<>();
    }

    private <T, R> R mapWithModelMapper(T source, Class<R> targetClass) {
        return modelMapper.map(source, targetClass);
    }

    // Team conversion methods
    private TeamDto convertPickleballTeamToTeamDto(PickleballTeamDto dto) {
        TeamDto out = mapWithModelMapper(dto, TeamDto.class);
        out.setBadmintonTournamentId(dto.getPickleballTournamentId());
        out.setPlayers(mapList(dto.getPlayers(), this::convertPickleballTeamPlayerDtoToTeamPlayerDto));
        return out;
    }

    private PickleballTeamDto convertTeamToPickleballTeamDto(TeamDto dto) {
        PickleballTeamDto out = mapWithModelMapper(dto, PickleballTeamDto.class);
        setPickleballTournamentId(out, dto.getBadmintonTournamentId());
        out.setPlayers(mapList(dto.getPlayers(), this::convertTeamPlayerDtoToPickleballTeamPlayerDto));
        return out;
    }

    private PickleballTeamDto convertBadmintonTeamToPickleballTeamDto(TeamDto dto) {
        PickleballTeamDto out = mapWithModelMapper(dto, PickleballTeamDto.class);
        setPickleballTournamentId(out, dto.getBadmintonTournamentId());

        List<PickleBallTeamPlayerDto> teamPlayers = mapList(dto.getPlayers(),
                p -> mapWithModelMapper(p, PickleBallTeamPlayerDto.class));
        out.setPlayers(teamPlayers);

        return out;
    }

    private void setPickleballTournamentId(PickleballTeamDto dto, String badmintonTournamentId) {
        if (StringUtils.hasText(badmintonTournamentId)) {
            dto.setPickleballTournamentId(badmintonTournamentId);
        }
    }

    // Player conversion methods
    public TeamPlayerDto convertPickleballTeamPlayerDtoToTeamPlayerDto(PickleBallTeamPlayerDto dto) {
        return mapWithModelMapper(dto, TeamPlayerDto.class);
    }

    public PickleBallTeamPlayerDto convertTeamPlayerDtoToPickleballTeamPlayerDto(TeamPlayerDto dto) {
        return mapWithModelMapper(dto, PickleBallTeamPlayerDto.class);
    }

    // Team DTO conversion methods
    public TeamDto convertPickleballTeamDtoToTeamDto(PickleballTeamDto dto) {
        return convertPickleballTeamToTeamDto(dto);
    }

    public PickleballTeamDto convertTeamDtoToPickleballTeamDto(TeamDto dto) {
        return convertTeamToPickleballTeamDto(dto);
    }

    // Match conversion methods
    public BadmintonMatchDto convertPickleballMatchDtoToBadmintonMatchDto(PickleballMatchDto dto) {
        BadmintonMatchDto out = mapWithModelMapper(dto, BadmintonMatchDto.class);
        out.setTeams(mapList(dto.getTeams(), this::convertPickleballTeamDtoToTeamDto));
        out.setPlayers(mapList(dto.getPlayers(), this::convertPickleballTeamPlayerDtoToTeamPlayerDto));

        out.setRounds(mapList(dto.getRounds(), this::convertPickleballPlayerScoreToBadmintonPlayerScore));

        return out;
    }

    public PickleballMatchDto convertBadmintonMatchDtoToPickleballMatchDto(BadmintonMatchDto dto) {
        PickleballMatchDto out = mapWithModelMapper(dto, PickleballMatchDto.class);
        out.setPlayers(mapList(dto.getPlayers(), this::convertTeamPlayerDtoToPickleballTeamPlayerDto));
        out.setTeams(mapList(dto.getTeams(), this::convertTeamDtoToPickleballTeamDto));
        return out;
    }

    // Create/Update Match DTO conversion methods
    public CreatePickleballMatchDto convertCreateBadmintonMatchDtoToCreatePickleballMatchDto(
            CreateBadmintonMatchDto dto) {
        CreatePickleballMatchDto out = mapWithModelMapper(dto, CreatePickleballMatchDto.class);
        out.setTeamIds(mapList(dto.getTeamIds(), this::convertBadmintonTeamToPickleballTeamDto));
        out.setPlayers(mapList(dto.getPlayers(), p -> mapWithModelMapper(p, PickleBallTeamPlayerDto.class)));
        return out;
    }

    public UpdatePickleballMatchDto convertUpdateBadmintonMatchDtoToUpdatePickleballMatchDto(
            UpdateBadmintonMatchDto dto) {
        UpdatePickleballMatchDto out = mapWithModelMapper(dto, UpdatePickleballMatchDto.class);
        out.setTeamIds(mapList(dto.getTeamIds(), this::convertBadmintonTeamToPickleballTeamDto));
        out.setPlayers(mapList(dto.getPlayers(), p -> mapWithModelMapper(p, PickleBallTeamPlayerDto.class)));
        return out;
    }

    // Score request conversion
    public PickleballScoreRequestDto convertBadmintonScoreRequestDtoToPickleballScoreRequestDto(
            BadmintonScoreRequestDto dto) {
        return mapWithModelMapper(dto, PickleballScoreRequestDto.class);
    }

    // Match detail conversion methods
    public BadmintonMatchDetailDto convertPickleballMatchDetailDtoToBadmintonMatchDetailDto(
            PickleballMatchDetailDto dto) {
        BadmintonMatchDetailDto out = mapWithModelMapper(dto, BadmintonMatchDetailDto.class);
        out.setRounds(mapList(dto.getRounds(), this::convertPickleballRoundToBadmintonRound));
        out.setWinningTeam(convertPickleballTeamToTeamDtoIfNotNull(dto.getWinningTeam()));
        return out;
    }

    private BadmintonMatchRoundDetailsDto convertPickleballRoundToBadmintonRound(
            PickleballMatchRoundDetailsDto round) {
        BadmintonMatchRoundDetailsDto out = mapWithModelMapper(round, BadmintonMatchRoundDetailsDto.class);
        out.setPlayerScores(mapList(round.getPlayerScores(), this::convertPickleballPlayerScoreToBadmintonPlayerScore));
        out.setWinningTeam(convertPickleballTeamToTeamDtoIfNotNull(round.getWinningTeam()));
        return out;
    }

    private BadmintonPlayerScoreDto convertPickleballPlayerScoreToBadmintonPlayerScore(
            PickleballPlayerScoreDto score) {
        BadmintonPlayerScoreDto out = mapWithModelMapper(score, BadmintonPlayerScoreDto.class);
        out.setTeam(convertPickleballTeamToTeamDtoIfNotNull(score.getTeam()));
        return out;
    }

    private TeamDto convertPickleballTeamToTeamDtoIfNotNull(PickleballTeamDto team) {
        return team != null ? convertPickleballTeamToTeamDto(team) : null;
    }

    public PickleballMatchDetailRequestDto convertBadmintonMatchDetailsDtoToPickleballMatchDetailsDto(
            BadmintonMatchDetailRequestDto dto) {
        PickleballMatchDetailRequestDto out = mapWithModelMapper(dto, PickleballMatchDetailRequestDto.class);
        out.setRounds(mapList(dto.getRounds(), this::convertBadmintonRoundToPickleballRound));
        return out;
    }

    private PickleballMatchRoundDetailsRequestDto convertBadmintonRoundToPickleballRound(
            BadmintonMatchRoundDetailsRequestDto round) {
        PickleballMatchRoundDetailsRequestDto out = mapWithModelMapper(round,
                PickleballMatchRoundDetailsRequestDto.class);
        out.setPlayerScores(mapList(round.getPlayerScores(),
                s -> mapWithModelMapper(s, PickleballPlayerScoreRequestDto.class)));
        return out;
    }

    // Tournament conversion methods
    public UpdatePickleballTournamentDto convertUpdateBadmintonTournamentDtoToUpdatePickleballTournamentDto(
            UpdateBadmintonTournamentDto dto) {
        return mapWithModelMapper(dto, UpdatePickleballTournamentDto.class);
    }

    public BadmintonTournamentDto convertPickleballTournamentDtoToBadmintonTournamentDto(PickleballTournamentDto dto) {
        BadmintonTournamentDto out = mapWithModelMapper(dto, BadmintonTournamentDto.class);
        out.setMatches(mapList(dto.getMatches(), this::convertPickleballMatchDtoToBadmintonMatchDto));
        return out;
    }

    public PickleballTournamentDto convertBadmintonTournamentDtoToPickleballTournamentDto(BadmintonTournamentDto dto) {
        PickleballTournamentDto out = mapWithModelMapper(dto, PickleballTournamentDto.class);
        out.setMatches(mapList(dto.getMatches(), this::convertBadmintonMatchDtoToPickleballMatchDto));
        return out;
    }
}