package com.playmotech.api.core.constants;

public enum BadmintonEventType {
    // Match Flow
    MATCH_STARTED("Match started between {team1} and {team2}"),
    MATCH_ENDED("Match ended. {winner} wins the match!"),
    SET_STARTED("Set {setNumber} begins"),
    SET_ENDED("Set {setNumber} goes to {winner}"),

    // Scoring
    POINT_SCORED("{player} scores a point with a {shotType}!"),
    BREAK_POINT("Break point opportunity for {player}"),
    SET_POINT("Set point for {player}"),
    MATCH_POINT("Match point for {player}!"),
    DEUCE("Deuce! {score} all"),
    ADVANTAGE("{player} has the advantage"),

    // Shots
    SMASH("{player} with a powerful smash!"),
    DROP_SHOT("{player} executes a perfect drop shot"),
    CLEAR("{player} clears to the back of the court"),
    DRIVE("{player} with a fast drive down the line"),
    NET_SHOT("{player} plays a delicate net shot"),
    TUMBLING_NET_SHOT("What a tumbling net shot from {player}!"),

    // Game Events
    LET("Let! The point will be replayed"),
    FAULT("Fault! Point to {winner}"),
    CHALLENGE("{team} challenges the call..."),
    CHALLENGE_SUCCESSFUL("Challenge successful! The call is overturned"),
    CHALLENGE_UNSUCCESSFUL("Challenge unsuccessful. The original call stands"),
    TIME_OUT_CALLED("{team} calls a time out"),
    CHANGE_OF_ENDS("Players change ends"),

    // Player Actions
    ACE("{player} serves an ace!"),
    UNFORCED_ERROR("Unforced error from {player}"),
    RALLY("Great rally! {duration} shots played"),
    DEFENSIVE_RETURN("Incredible defensive return from {player}"),

    // Match Context
    COMEBACK("What a comeback from {player}!"),
    STREAK("{player} has won {points} points in a row"),
    MOMENTUM_SHIFT("The momentum is shifting in {player}'s favor");

    private final String defaultTemplate;

    BadmintonEventType(String defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public String getDefaultTemplate() {
        return defaultTemplate;
    }
}
