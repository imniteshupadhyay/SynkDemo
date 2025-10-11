package com.playmotech.api.core.constants;

public class BadmintonTemplateVariables {
    // Player/Team
    public static final String PLAYER = "player";
    public static final String PLAYER_1 = "player1";
    public static final String PLAYER_2 = "player2";
    public static final String TEAM_1 = "team1";
    public static final String TEAM_2 = "team2";
    public static final String WINNER = "winner";
    public static final String SERVER = "server";
    public static final String RECEIVER = "receiver";

    // Score
    public static final String SCORE = "score";
    public static final String PLAYER_1_SCORE = "player1Score";
    public static final String PLAYER_2_SCORE = "player2Score";
    public static final String PLAYER_1_SETS = "player1Sets";
    public static final String PLAYER_2_SETS = "player2Sets";
    public static final String CURRENT_SET = "currentSet";

    // Game Elements
    public static final String SHOT_TYPE = "shotType";
    public static final String RALLY_LENGTH = "rallyLength";
    public static final String MATCH_DURATION = "matchDuration";
    public static final String POINTS_WON = "pointsWon";
    public static final String CONSECUTIVE_POINTS = "consecutivePoints";

    // Templates
    public static final String POINT_TEMPLATE = "{player} scores with a {shotType}!";
    public static final String ACE_TEMPLATE = "Ace! {player} serves an unreturnable shot!";
    public static final String RALLY_TEMPLATE = "Incredible rally! {player} wins the point after {rallyLength} shots!";
}
