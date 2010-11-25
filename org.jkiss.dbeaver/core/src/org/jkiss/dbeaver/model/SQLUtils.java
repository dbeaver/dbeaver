/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL Utils
 */
public final class SQLUtils {

    static final Log log = LogFactory.getLog(SQLUtils.class);

    public static final String TOKEN_TRANSFORM_START = "/*DB[*/";
    public static final String TOKEN_TRANSFORM_END = "/*]DB*/";

    public static final Pattern PATTERN_XFORM = Pattern.compile(Pattern.quote(TOKEN_TRANSFORM_START) + "[^" + Pattern.quote(TOKEN_TRANSFORM_END) + "]*" + Pattern.quote(TOKEN_TRANSFORM_END));

    public static String stripTransformations(String query)
    {
        return PATTERN_XFORM.matcher(query).replaceAll("");
    }

    public static String stripComments(String query)
    {
        return stripComments(query, "/*", "*/", "--");
    }

    public static String stripComments(String query, String mlCommentStart, String mlCommentEnd, String slComment)
    {
        query = query.trim();
        Pattern stripPattern = Pattern.compile(
            "(\\s*" + Pattern.quote(mlCommentStart) +
            "[^" + Pattern.quote(mlCommentEnd) +
            "]*" + Pattern.quote(mlCommentEnd) +
            "\\s*)[^" + Pattern.quote(mlCommentStart) + "]*");
        Matcher matcher = stripPattern.matcher(query);
        if (matcher.matches()) {
            query = query.substring(matcher.end(1));
        }
        while (query.startsWith(slComment)) {
            int crPos = query.indexOf('\n');
            if (crPos == -1) {
                break;
            } else {
                query = query.substring(crPos).trim();
            }
        }
        return query;
    }

    public static boolean matchesLike(String string, String like)
    {
        like = like.replace("%", ".*").replace("_", ".");
        Pattern pattern = Pattern.compile(like, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        return pattern.matcher(string).matches();
    }
}