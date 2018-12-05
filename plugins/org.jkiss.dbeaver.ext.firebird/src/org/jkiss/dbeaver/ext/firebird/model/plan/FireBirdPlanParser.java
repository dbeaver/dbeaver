package org.jkiss.dbeaver.ext.firebird.model.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FireBirdPlanParser {

		private List<Matcher> matchers = new ArrayList<>(PlanToken.values().length);
		private Map<Matcher,PlanToken> matchertokens = new HashMap<>(PlanToken.values().length);
		private String plan;
		private int position = 0;
		private PlanTokenMatch tokenMatch;
		
		FireBirdPlanParser(String plan) {
			this.plan = plan;
			for (PlanToken token: PlanToken.values()) {
				Matcher matcher = token.newMatcher(plan);
				matchers.add(matcher);
				matchertokens.put(matcher, token);
			}
		}
		
/*
	PLAN <plan-expr>
	
	<plan-expr> ::=  (<plan-item> [, <plan-item> ...])
	                    | <sorted-item>
	                    | <joined-item>
	                    | <merged-item>
	                    
	<sorted-item> ::=  SORT (<plan-item>)
	
	<joined-item> ::=  JOIN (<plan-item>, <plan-item> [, <plan-item> ...])
	
	<merged-item> ::=  [SORT] MERGE (<sorted-item>, <sorted-item> [, <sorted-item> ...])
	
	<plan-item> ::= <basic-item> | <plan-expr>
	
	<basic-item> ::= <relation>
	                    {NATURAL
	                     | INDEX (<indexlist>)
	                     | ORDER index [INDEX (<indexlist>)]}
	                     
	<relation> ::=  table | view [table]
						
	<indexlist> ::= index [, index ...]
	    	
*/

		FireBirdPlanNode parse() throws FireBirdPlanException {
			tokenMatch = jump();
			checkToken(PlanToken.PLAN, tokenMatch);
			FireBirdPlanNode node = addPlanNode(null, plan);
			tokenMatch = jump();
			planExpr(node);
			return node;
		}	
		
		private void planExpr(FireBirdPlanNode parent) throws FireBirdPlanException {
			switch (tokenMatch.token) {
				case LEFTPARENTHESE:
					do {
						tokenMatch = jump();
						planItem(parent);
					} while (tokenMatch.token == PlanToken.COMMA);
					checkToken(PlanToken.RIGHTPARENTHESE, tokenMatch);
					break;
				case SORT:
					sortedItem(parent);
					break;
				case JOIN:
					joinedItem(parent);
					break;
				case SORT_MERGE:
					mergedItem(parent, true);
					break;
				case MERGE:
					mergedItem(parent, false);
					break;					
				default:
					raisePlanTokenException(tokenMatch);
			}
		}
		
		private void planItem(FireBirdPlanNode parent) throws FireBirdPlanException {
			switch (tokenMatch.token) {
				case IDENTIFICATOR:
					basicItem(parent);
					break;
				default:
					planExpr(parent);
					break;
			}
		}
		
		private void joinedItem(FireBirdPlanNode parent) throws FireBirdPlanException {
			checkToken(PlanToken.JOIN, tokenMatch);
			FireBirdPlanNode node = addPlanNode(parent, "JOIN");
			tokenMatch = jump();
			checkToken(PlanToken.LEFTPARENTHESE, tokenMatch);
			do {
				tokenMatch = jump();
				planItem(node);
				tokenMatch = jump();
			} while (tokenMatch.getToken() == PlanToken.COMMA);
			checkToken(PlanToken.RIGHTPARENTHESE, tokenMatch);
		}
			
		private void mergedItem(FireBirdPlanNode parent, Boolean sorted) throws FireBirdPlanException {
			if (sorted) {
				checkToken(PlanToken.SORT_MERGE, tokenMatch);
			} else {
				checkToken(PlanToken.MERGE, tokenMatch);
			}
			tokenMatch = jump();
			checkToken(PlanToken.LEFTPARENTHESE, tokenMatch);
			FireBirdPlanNode node = null;
			if (sorted) {
				node = addPlanNode(parent, "SORT MERGE");
			} else {
				node = addPlanNode(parent, "MERGE");
			}
			tokenMatch = jump();
			checkToken(PlanToken.LEFTPARENTHESE, tokenMatch);
			do {
				tokenMatch = jump();
				sortedItem(node);
				tokenMatch = jump();
			} while (tokenMatch.getToken() == PlanToken.COMMA);
			checkToken(PlanToken.RIGHTPARENTHESE, tokenMatch);
		}
		
		private void sortedItem(FireBirdPlanNode parent) throws FireBirdPlanException {
			checkToken(PlanToken.SORT, tokenMatch);
			FireBirdPlanNode node = addPlanNode(parent, "SORT");
			tokenMatch = jump();
			checkToken(PlanToken.LEFTPARENTHESE, tokenMatch);
			tokenMatch = jump();
			planItem(node);
			checkToken(PlanToken.RIGHTPARENTHESE, tokenMatch);			
		}
		
		private void basicItem(FireBirdPlanNode parent) throws FireBirdPlanException {
			String aliases = collectIdentifiers();
			switch (tokenMatch.token) {
				case NATURAL:
					addPlanNode(parent, aliases + " NATURAL");
					break;
				case INDEX:
					String indexes = collectIndexes();
					addPlanNode(parent, aliases + " INDEX(" + indexes + ")");
					break;
				case ORDER:
					tokenMatch = jump();
					checkToken(PlanToken.IDENTIFICATOR, tokenMatch);
					String orderIndex = tokenMatch.getValue();
					tokenMatch = jump();
					String text = aliases + " ORDER " + orderIndex;
					if (tokenMatch.getToken() == PlanToken.INDEX) {
						String orderIndexes = collectIndexes();
						text = text + " INDEX(" + orderIndexes + ")";
					}
					addPlanNode(parent, text);
					break;
			default:
				raisePlanTokenException(tokenMatch);
			}
			
		}

		private String collectIdentifiers() {
			String identifiers = "";
			while (tokenMatch.getToken() == PlanToken.IDENTIFICATOR) {
				identifiers = identifiers + tokenMatch.getValue() + " ";
				tokenMatch = jump();
			};
			return identifiers;
		}
		
		private String collectIndexes() throws FireBirdPlanException {
			tokenMatch = jump();
			checkToken(PlanToken.LEFTPARENTHESE, tokenMatch);
			String indexes = "";
			tokenMatch = jump();
			while (tokenMatch.getToken() != PlanToken.RIGHTPARENTHESE) {
				indexes = indexes + tokenMatch.getValue();
				tokenMatch = jump();
				if(tokenMatch.getToken() == PlanToken.COMMA) {
					indexes = indexes + ",";
					tokenMatch = jump();
				}
			};
			return indexes;
		}
		
		private PlanTokenMatch find() {
			for (Matcher matcher: matchers) {
				if (matcher.find(position)) {
					position = position + matcher.group().length();
					return new PlanTokenMatch(matchertokens.get(matcher), matcher.group());
				}
			}
			return new PlanTokenMatch(PlanToken.IDENTIFICATOR, "???");
		}
		
		private PlanTokenMatch jump() {
			PlanTokenMatch tokenMatch = null;
			do {
				tokenMatch = find();
			} while (tokenMatch.getToken() == PlanToken.WHITESPACE);
			return tokenMatch;
		}
		
		private void checkToken(PlanToken token, PlanTokenMatch tokenMatch) throws FireBirdPlanException {
			if (token != tokenMatch.getToken()) {
				raisePlanTokenException(token, tokenMatch);
			}
		}
		
		private void raisePlanTokenException(PlanToken token, PlanTokenMatch tokenMatch) throws FireBirdPlanException {
			throw new FireBirdPlanException(token.toString(), tokenMatch.getToken().toString(), 
					position - tokenMatch.getValue().length(), plan);
		}
		
		private void raisePlanTokenException(PlanTokenMatch tokenMatch) throws FireBirdPlanException {
			throw new FireBirdPlanException(tokenMatch.getToken().toString(), 
					position - tokenMatch.getValue().length(), plan);
		}
		
		private FireBirdPlanNode addPlanNode(FireBirdPlanNode parent, String text) {
			FireBirdPlanNode node;
			node = new FireBirdPlanNode(text);
			node.parent = parent;
			if (parent != null) {
				parent.getNested().add(node);
			}
			return node;
		}
		
		enum PlanToken {
			PLAN("\\GPLAN\\b"), 
			JOIN("\\GJOIN\\b"), 
			NATURAL("\\GNATURAL\\b"),
			SORT_MERGE("\\GSORT\\w+MERGE\\b"),
			SORT("\\GSORT\\b"), 
			MERGE("\\GMERGE\\b"), 
			ORDER("\\GORDER\\b"), 
			INDEX("\\GINDEX\\b"), 
			LEFTPARENTHESE("\\G\\("), 
			RIGHTPARENTHESE("\\G\\)"),
			COMMA("\\G,"),
			WHITESPACE("\\G\\s+"),
			IDENTIFICATOR("\\G\\b[\\w$]+\\b"),
			UNRECOGNIZED("\\G\\b[^\\s]+\\b");
		    
			private final Pattern pattern;

		    private PlanToken(String regex) {
		        pattern = Pattern.compile(regex);
		    }
		    
		    public Matcher newMatcher(String text) {
		    	Matcher matcher = pattern.matcher(text);
		        return matcher;
		    }
		}
		
		class PlanTokenMatch {
			
			private PlanToken token;
			private String value;

			public PlanTokenMatch(PlanToken token, String value) {
				super();
				this.token = token;
				this.value = value;
			}

			public PlanToken getToken() {
				return token;
			}

			public String getValue() {
				return value;
			}
		}
		
	}