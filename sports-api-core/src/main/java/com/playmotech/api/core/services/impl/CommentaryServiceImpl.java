package com.playmotech.api.core.services.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.playmotech.api.core.constants.BadmintonEventType;
import com.playmotech.api.core.constants.BadmintonTemplateVariables;
import com.playmotech.api.core.dto.BadmintonMatchContext;
import com.playmotech.api.core.services.CommentaryService;

@Service
public class CommentaryServiceImpl implements CommentaryService {

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^}]*)\\}");

	@Override
	public String generateCommentary(BadmintonEventType eventType, BadmintonMatchContext matchContext) {
		String template = eventType.getDefaultTemplate();
		Map<String, String> variables = extractVariablesFromMatchContext(matchContext);
		return generateCommentary(template, variables);
	}

	@Override
	public String generateCommentary(String template, Map<String, String> variables) {
		if (template == null || template.isEmpty()) {
			return "";
		}

		// Simple template variable replacement
		String result = template;
		Matcher matcher = VARIABLE_PATTERN.matcher(template);

		while (matcher.find()) {
			String variableName = matcher.group(1);
			String value = variables.getOrDefault(variableName, "");
			result = result.replace("{" + variableName + "}", value != null ? value : "");
		}

		return result;
	}

	/**
	 * Extracts variables from the match context to be used in templates.
	 * 
	 * @param matchContext The match context
	 * @return Map of variable names to their values
	 */
	private Map<String, String> extractVariablesFromMatchContext(BadmintonMatchContext matchContext) {
		Map<String, String> variables = new HashMap<>();

		// Player/Team information
		variables.put(BadmintonTemplateVariables.PLAYER_1, matchContext.getPlayer1());
		variables.put(BadmintonTemplateVariables.PLAYER_2, matchContext.getPlayer2());
		variables.put(BadmintonTemplateVariables.TEAM_1, matchContext.getTeam1());
		variables.put(BadmintonTemplateVariables.TEAM_2, matchContext.getTeam2());
		variables.put(BadmintonTemplateVariables.SERVER, matchContext.getServingPlayer());
		variables.put(BadmintonTemplateVariables.RECEIVER, matchContext.getReceivingPlayer());

		// Use the currentPlayer directly - it's now properly set in
		// BadmintonMatchService
		variables.put(BadmintonTemplateVariables.PLAYER, matchContext.getCurrentPlayer());

		// Score information
		variables.put(BadmintonTemplateVariables.SCORE,
				matchContext.getPlayer1Score() + "-" + matchContext.getPlayer2Score());
		variables.put(BadmintonTemplateVariables.PLAYER_1_SCORE, String.valueOf(matchContext.getPlayer1Score()));
		variables.put(BadmintonTemplateVariables.PLAYER_2_SCORE, String.valueOf(matchContext.getPlayer2Score()));
		variables.put(BadmintonTemplateVariables.PLAYER_1_SETS, String.valueOf(matchContext.getPlayer1Sets()));
		variables.put(BadmintonTemplateVariables.PLAYER_2_SETS, String.valueOf(matchContext.getPlayer2Sets()));
		variables.put(BadmintonTemplateVariables.CURRENT_SET, String.valueOf(matchContext.getCurrentSet()));

		// Game elements
		variables.put(BadmintonTemplateVariables.SHOT_TYPE,
				matchContext.getShotType() != null ? matchContext.getShotType() : matchContext.getLastShotType());
		variables.put(BadmintonTemplateVariables.RALLY_LENGTH, String.valueOf(matchContext.getRallyLength()));

		// Determine winner based on score (simplified logic)
		String winner = matchContext.getPlayer1Score() > matchContext.getPlayer2Score() ? matchContext.getPlayer1()
				: matchContext.getPlayer2();
		variables.put(BadmintonTemplateVariables.WINNER, winner);

		return variables;
	}
}
