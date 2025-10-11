WITH match_details AS (
    SELECT
        bm.id AS match_id,
        bmpd.winning_team_id,
        bmpd.winning_player_id,
        bmpd.winning_guest_player_name,
        t.team_name AS winning_team_name,
        up.display_name AS winning_player_name,
        COALESCE(SUM(bmrpd.score), 0) AS total_points,
        CASE
            WHEN bmpd.winning_player_id IS NOT NULL THEN 'REGISTERED'
            WHEN bmpd.winning_guest_player_name IS NOT NULL THEN 'GUEST'
            ELSE 'UNKNOWN'
        END AS winner_type
    FROM badminton_matches bm
    LEFT JOIN badminton_match_play_details bmpd ON bmpd.match_id = bm.id
    LEFT JOIN teams t ON t.id = bmpd.winning_team_id
    LEFT JOIN user_profiles up ON up.id = bmpd.winning_player_id
    LEFT JOIN badminton_match_rounds bmr ON bmr.match_play_details_id = bmpd.id
    LEFT JOIN badminton_match_round_play_details bmrpd ON bmrpd.match_round_id = bmr.id
    WHERE bm.badminton_tournament_id = '61bb1630-ffa2-4d73-9de2-fcaece1d07b2'
    GROUP BY bm.id, bmpd.winning_team_id, bmpd.winning_player_id,
             bmpd.winning_guest_player_name, t.team_name, up.display_name
),
player_participation AS (
    SELECT
        bspm.match_id,
        up.id AS player_id,
        up.display_name AS player_name,
        bspm.guest_name AS guest_player_name
    FROM badminton_singles_player_mapping bspm
    LEFT JOIN user_profiles up ON up.id = bspm.player_user_id
    WHERE bspm.match_id IN (
        SELECT id FROM badminton_matches
        WHERE badminton_tournament_id = '61bb1630-ffa2-4d73-9de2-fcaece1d07b2'
    )
),
team_participation AS (
    SELECT
        btm.match_id,
        t.id AS team_id,
        t.team_name
    FROM badminton_team_mapping btm
    LEFT JOIN teams t ON t.id = btm.team_id
    WHERE btm.match_id IN (
        SELECT id FROM badminton_matches
        WHERE badminton_tournament_id = '61bb1630-ffa2-4d73-9de2-fcaece1d07b2'
    )
),
player_performance AS (
    SELECT
        pp.player_id,
        pp.player_name,
        COUNT(DISTINCT pp.match_id) AS matches_played,
        COUNT(DISTINCT md.match_id) AS matches_won,
        SUM(md.total_points) AS total_points
    FROM player_participation pp
    LEFT JOIN match_details md ON pp.match_id = md.match_id AND pp.player_id = md.winning_player_id
    WHERE pp.player_id IS NOT NULL
    GROUP BY pp.player_id, pp.player_name
),
guest_player_performance AS (
    SELECT
        pp.guest_player_name,
        COUNT(DISTINCT pp.match_id) AS matches_played,
        COUNT(DISTINCT md.match_id) AS matches_won,
        SUM(md.total_points) AS total_points
    FROM player_participation pp
    LEFT JOIN match_details md ON pp.match_id = md.match_id AND pp.guest_player_name = md.winning_guest_player_name
    WHERE pp.guest_player_name IS NOT NULL
    GROUP BY pp.guest_player_name
),
team_performance AS (
    SELECT
        tp.team_id,
        tp.team_name,
        COUNT(DISTINCT tp.match_id) AS matches_played,
        COUNT(DISTINCT md.match_id) AS matches_won
    FROM team_participation tp
    LEFT JOIN match_details md ON tp.match_id = md.match_id AND tp.team_id = md.winning_team_id
    GROUP BY tp.team_id, tp.team_name
),
top_registered_players AS (
    SELECT * FROM (
        SELECT
            player_id,
            player_name,
            matches_played,
            matches_won,
            total_points,
            ROW_NUMBER() OVER (ORDER BY matches_won DESC, total_points DESC) AS rank
        FROM player_performance
    ) ranked_players
    WHERE rank <= 10
),
top_guest_players AS (
    SELECT * FROM (
        SELECT
            guest_player_name,
            matches_played,
            matches_won,
            total_points,
            ROW_NUMBER() OVER (ORDER BY matches_won DESC, total_points DESC) AS rank
        FROM guest_player_performance
    ) ranked_guests
    WHERE rank <= 10
)
SELECT
    bt.id AS tournament_id,
    bt.name AS tournament_name,
    (SELECT COUNT(DISTINCT id) FROM badminton_matches
     WHERE badminton_tournament_id = '61bb1630-ffa2-4d73-9de2-fcaece1d07b2') AS total_matches_played,
    (SELECT COUNT(DISTINCT id) FROM badminton_matches
     WHERE badminton_tournament_id = '61bb1630-ffa2-4d73-9de2-fcaece1d07b2'
     AND match_status = 'COMPLETED') AS total_matches_completed,
    (SELECT JSON_AGG(
        JSON_BUILD_OBJECT(
            'playerId', player_id,
            'playerName', player_name,
            'matchesPlayed', matches_played,
            'matchesWon', matches_won,
            'winPercentage', ROUND(100.0 * matches_won / matches_played, 2),
            'totalPoints', total_points
        )
    ) FROM top_registered_players) AS top_player_performances,
    (SELECT JSON_AGG(
        JSON_BUILD_OBJECT(
            'guestPlayerName', guest_player_name,
            'matchesPlayed', matches_played,
            'matchesWon', matches_won,
            'winPercentage', ROUND(100.0 * matches_won / matches_played, 2),
            'totalPoints', total_points
        )
    ) FROM top_guest_players) AS top_guest_player_performances,
    (SELECT JSON_AGG(
        JSON_BUILD_OBJECT(
            'teamId', team_id,
            'teamName', team_name,
            'matchesPlayed', matches_played,
            'matchesWon', matches_won,
            'winPercentage', ROUND(100.0 * matches_won / matches_played, 2)
        )
    ) FROM team_performance) AS team_performances
FROM badminton_tournaments bt
WHERE bt.id = '61bb1630-ffa2-4d73-9de2-fcaece1d07b2';