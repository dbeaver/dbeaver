package org.jkiss.dbeaver.ext.firebird.model.plan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum FireBirdPlanToken {
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

    private FireBirdPlanToken(String regex) {
        pattern = Pattern.compile(regex);
    }
    
    public Matcher newMatcher(String text) {
    	Matcher matcher = pattern.matcher(text);
        return matcher;
    }
}