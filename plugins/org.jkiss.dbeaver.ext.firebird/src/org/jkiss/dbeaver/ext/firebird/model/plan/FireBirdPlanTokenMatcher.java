package org.jkiss.dbeaver.ext.firebird.model.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

class FireBirdPlanTokenMatcher {

	private List<Matcher> matchers = new ArrayList<>(FireBirdPlanToken.values().length);
	private Map<Matcher,FireBirdPlanToken> matchertokens = new HashMap<>(FireBirdPlanToken.values().length);
	private int position = 0;
	FireBirdPlanToken token;
	private String value;
	private String subject;
	
	public FireBirdPlanTokenMatcher(String subject) {
		super();
		this.subject = subject;
		for (FireBirdPlanToken token: FireBirdPlanToken.values()) {
			Matcher matcher = token.newMatcher(subject);
			matchers.add(matcher);
			matchertokens.put(matcher, token);
		}
	}

	FireBirdPlanToken getToken() {
		return token;
	}

	String getValue() {
		return value;
	}

	void find() {
		for (Matcher matcher: matchers) {
			if (matcher.find(position)) {
				position = position + matcher.group().length();
				token = matchertokens.get(matcher);
				value = matcher.group();
				return;
			}
		}
		token = FireBirdPlanToken.IDENTIFICATOR;
		value = "???";
		return;
	}
	
	void jump() {
		do {
			find();
		} while (token == FireBirdPlanToken.WHITESPACE);
		return;
	}
	
	void checkToken(FireBirdPlanToken expected) throws FireBirdPlanException {
		if (expected != this.token) {
			raisePlanTokenException(expected, this.token);
		}
	}
	
	void raisePlanTokenException(FireBirdPlanToken expected, FireBirdPlanToken actual) throws FireBirdPlanException {
		throw new FireBirdPlanException(token.toString(), actual.toString(), 
				position - value.length(), subject);
	}
	
	void raisePlanTokenException() throws FireBirdPlanException {
		throw new FireBirdPlanException(token.toString(), 
				position - value.length(), subject);
	}			
}