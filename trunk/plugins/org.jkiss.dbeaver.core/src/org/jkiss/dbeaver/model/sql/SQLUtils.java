/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.dbeaver.ui.editors.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.ui.editors.sql.format.tokenized.SQLTokenizedFormatter;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL Utils
 */
public final class SQLUtils {

    //static final Log log = LogFactory.getLog(SQLUtils.class);

    public static final String TOKEN_TRANSFORM_START = "/*DB[*/";
    public static final String TOKEN_TRANSFORM_END = "/*]DB*/";

    public static final Pattern PATTERN_XFORM = Pattern.compile(Pattern.quote(TOKEN_TRANSFORM_START) + "[^" + Pattern.quote(TOKEN_TRANSFORM_END) + "]*" + Pattern.quote(TOKEN_TRANSFORM_END));

    public static final Pattern PATTERN_OUT_PARAM = Pattern.compile("((\\?)|(:[a-z0-9]+))\\s*:=");

    public static String stripTransformations(String query)
    {
        if (!query.contains(TOKEN_TRANSFORM_START)) {
            return query;
        } else {
            return PATTERN_XFORM.matcher(query).replaceAll("");
        }
    }

    public static boolean isDataModifyQuery(String query)
    {
        query = query.trim().toUpperCase();
        return query.startsWith("UPDATE") || query.startsWith("INSERT") || query.startsWith("DELETE");
    }

    public static String stripComments(SQLDataSource dataSource, String query)
    {
        SQLDialect dialect = dataSource.getSQLDialect();
        Pair<String, String> multiLineComments = dialect.getMultiLineComments();
        return stripComments(
            query,
            multiLineComments == null ? null : multiLineComments.getFirst(),
            multiLineComments == null ? null : multiLineComments.getSecond(),
            dialect.getSingleLineComments());
    }

    public static String stripComments(String query, @Nullable String mlCommentStart, @Nullable String mlCommentEnd, String[] slComments)
    {
        query = query.trim();
        if (mlCommentStart != null && mlCommentEnd != null) {
            Pattern stripPattern = Pattern.compile(
                "(\\s*" + Pattern.quote(mlCommentStart) +
                    "[^" + Pattern.quote(mlCommentEnd) +
                    "]*" + Pattern.quote(mlCommentEnd) +
                    "\\s*)[^" + Pattern.quote(mlCommentStart) + "]*");
            Matcher matcher = stripPattern.matcher(query);
            if (matcher.matches()) {
                query = query.substring(matcher.end(1));
            }
        }
        for (String slComment : slComments) {
            while (query.startsWith(slComment)) {
                int crPos = query.indexOf('\n');
                if (crPos == -1) {
                    break;
                } else {
                    query = query.substring(crPos).trim();
                }
            }
        }
        return query;
    }

    public static List<String> splitFilter(String filter)
    {
        if (CommonUtils.isEmpty(filter)) {
            return Collections.emptyList();
        }
        return CommonUtils.splitString(filter, ',');
    }

    public static boolean matchesAnyLike(String string, Collection<String> likes)
    {
        for (String like : likes) {
            if (matchesLike(string, like)) {
                return true;
            }
        }
        return false;
    }

    public static String makeLikePattern(String like)
    {
        return like.replace("%", ".*").replace("_", ".?");
    }

    public static boolean matchesLike(String string, String like)
    {
        Pattern pattern = Pattern.compile(makeLikePattern(like), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        return pattern.matcher(string).matches();
    }

    public static void appendValue(StringBuilder buffer, DBSTypedObject type, Object value)
    {
        if (type.getDataKind() == DBPDataKind.NUMERIC || type.getDataKind() == DBPDataKind.BOOLEAN) {
            buffer.append(value);
        } else {
            buffer.append('\'').append(value).append('\'');
        }
    }

    public static String escapeString(String string)
    {
        return string.replaceAll("'", "\\'");
    }

    public static String getFirstKeyword(String query)
    {
        int startPos = 0, endPos = -1;
        for (int i = 0; i < query.length(); i++) {
            if (Character.isLetterOrDigit(query.charAt(i))) {
                startPos = i;
                break;
            }
        }
        for (int i = startPos; i < query.length(); i++) {
            if (Character.isWhitespace(query.charAt(i))) {
                endPos = i;
                break;
            }
        }
        if (endPos == -1) {
            return query;
        }
        return query.substring(startPos, endPos);
    }

    @Nullable
    public static String getQueryOutputParameter(DBCSession session, String query)
    {
        final Matcher matcher = PATTERN_OUT_PARAM.matcher(query);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Removes \\r characters from query.
     * Actually this is done specially for Oracle due to some bug in it's driver
     *
     * @param query query
     * @return normalized query
     */
    public static String makeUnifiedLineFeeds(String query)
    {
        if (query.indexOf('\r') == -1) {
            return query;
        }
        StringBuilder result = new StringBuilder(query.length());
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c == '\r') {
                continue;
            }
            result.append(c);
        }
        return result.toString();
    }

    public static String formatSQL(SQLDataSource dataSource, String query)
    {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.setDataSource(false, dataSource);
        SQLFormatterConfiguration configuration = new SQLFormatterConfiguration(syntaxManager);
        configuration.setKeywordCase(SQLFormatterConfiguration.KEYWORD_UPPER_CASE);
        return new SQLTokenizedFormatter().format(query, configuration);
    }

    public static void appendLikeCondition(StringBuilder sql, String value, boolean not)
    {
        if (value.contains("%") || value.contains("_")) {
            if (not) sql.append(" NOT");
            sql.append(" LIKE ?");
        }  else {
            sql.append(not ? "<>?": "=?");
        }
    }

    public static boolean appendFirstClause(StringBuilder sql, boolean firstClause)
    {
        if (firstClause) {
            sql.append(" WHERE ");
        } else {
            sql.append(" AND ");
        }
        return false;
    }

    public static String trimQueryStatement(SQLSyntaxManager syntaxManager, String sql)
    {
        sql = sql.trim();
        String statementDelimiter = syntaxManager.getStatementDelimiter();
        if (sql.endsWith(statementDelimiter)) {
            // Remove trailing delimiter only if it is not block end
            String trimmed = sql.substring(0, sql.length() - statementDelimiter.length());
            String test = trimmed.toUpperCase().trim();
            if (!test.endsWith(SQLConstants.BLOCK_END)) {
                sql = trimmed;
            }
        }
        return sql;
    }

    @Nullable
    public static SQLDialect getDialectFromObject(DBPObject object)
    {
        if (object instanceof DBSObject) {
            DBPDataSource dataSource = ((DBSObject)object).getDataSource();
            if (dataSource instanceof SQLDataSource) {
                return ((SQLDataSource) dataSource).getSQLDialect();
            }
        }
        return null;
    }

}