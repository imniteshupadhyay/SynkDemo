-- STATS QUERY
SELECT
  '2e1ee65a-e2f5-4c86-af07-6181fc44ffb1' as player_user_id,
  COUNT(*) as total_matches_played,
  SUM(CASE
    WHEN bmpd.winning_player_id = '2e1ee65a-e2f5-4c86-af07-6181fc44ffb1'
    THEN 1
    ELSE 0
  END) as matches_won,
  SUM(CASE
    WHEN COALESCE(bmpd.is_tied, false) = false
         AND (
           (bmpd.winning_player_id IS NOT NULL AND bmpd.winning_player_id != '2e1ee65a-e2f5-4c86-af07-6181fc44ffb1')
           OR (bmpd.winning_guest_player_name IS NOT NULL)
           OR (bmpd.winning_team_id IS NOT NULL)
         )
    THEN 1
    ELSE 0
  END) as matches_lost,
  SUM(CASE
    WHEN COALESCE(bmpd.is_tied, false) = true
    THEN 1
    ELSE 0
  END) as matches_tied,
  SUM(CASE
    WHEN bmpd.winning_player_id IS NULL
         AND bmpd.winning_guest_player_name IS NULL
         AND bmpd.winning_team_id IS NULL
         AND COALESCE(bmpd.is_tied, false) = false
    THEN 1
    ELSE 0
  END) as matches_without_result
FROM badminton_singles_player_mapping bspm
JOIN badminton_matches bm ON bspm.match_id = bm.id
LEFT JOIN badminton_match_play_details bmpd ON bm.id = bmpd.match_id
WHERE bspm.player_user_id = '2e1ee65a-e2f5-4c86-af07-6181fc44ffb1'
  AND COALESCE(bm.inactive, false) = false;


--DEBUG QUERY
SELECT
  bm.id as match_id,
  bm.match_status,
  bmpd.winning_player_id,
  bmpd.is_tied,
  CASE
    WHEN bmpd.winning_player_id = '2e1ee65a-e2f5-4c86-af07-6181fc44ffb1' THEN 'WON'
    WHEN bmpd.winning_player_id IS NOT NULL AND bmpd.winning_player_id != '2e1ee65a-e2f5-4c86-af07-6181fc44ffb1' AND COALESCE(bmpd.is_tied, false) = false THEN 'LOST'
    WHEN COALESCE(bmpd.is_tied, false) = true THEN 'TIED'
    ELSE 'NO_RESULT'
  END as match_result
FROM badminton_singles_player_mapping bspm
JOIN badminton_matches bm ON bspm.match_id = bm.id
LEFT JOIN badminton_match_play_details bmpd ON bm.id = bmpd.match_id
WHERE bspm.player_user_id = '2e1ee65a-e2f5-4c86-af07-6181fc44ffb1'
  AND COALESCE(bm.inactive, false) = false
ORDER BY bm.created_at_timestamp_utc DESC;