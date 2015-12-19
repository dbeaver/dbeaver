/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.sql;

import org.eclipse.core.resources.IFile;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.model.sql.format.tokenized.SQLTokenizedFormatter;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL Utils
 */
public final class SQLUtils {

    static final Log log = Log.getLog(SQLUtils.class);

    public static final Pattern PATTERN_OUT_PARAM = Pattern.compile("((\\?)|(:[a-z0-9]+))\\s*:=");
    public static final Pattern CREATE_PREFIX_PATTERN = Pattern.compile("(CREATE (:OR REPLACE)?).+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    public static final int MIN_SQL_DESCRIPTION_LENGTH = 512;
    public static final int MAX_SQL_DESCRIPTION_LENGTH = 500;

    public static String stripTransformations(String query)
    {
        return query;
//        if (!query.contains(TOKEN_TRANSFORM_START)) {
//            return query;
//        } else {
//            return PATTERN_XFORM.matcher(query).replaceAll("");
//        }
    }

    public static String stripComments(SQLDialect dialect, String query)
    {
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

    public static String quoteString(String string)
    {
        return "'" + string.replaceAll("'", "''") + "'";
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
        syntaxManager.init(dataSource.getSQLDialect(), dataSource.getContainer().getPreferenceStore());
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
        for (String statementDelimiter : syntaxManager.getStatementDelimiters()) {
            if (sql.endsWith(statementDelimiter) && sql.length() > statementDelimiter.length()) {
                if (Character.isAlphabetic(statementDelimiter.charAt(0))) {
                    // Delimiter is alphabetic (e.g. "GO") so it must be prefixed with whitespace
                    char lastChar = sql.charAt(sql.length() - statementDelimiter.length() - 1);
                    if (Character.isUnicodeIdentifierPart(lastChar)) {
                        return sql;
                    }
                }
                // Remove trailing delimiter only if it is not block end
                String trimmed = sql.substring(0, sql.length() - statementDelimiter.length());
                String test = trimmed.toUpperCase().trim();
                if (!test.endsWith(SQLConstants.BLOCK_END)) {
                    sql = trimmed;
                }
            }
        }
        return sql;
    }

    @NotNull
    public static SQLDialect getDialectFromObject(DBPObject object)
    {
        if (object instanceof DBSObject) {
            DBPDataSource dataSource = ((DBSObject)object).getDataSource();
            if (dataSource instanceof SQLDataSource) {
                return ((SQLDataSource) dataSource).getSQLDialect();
            }
        }
        return BasicSQLDialect.INSTANCE;
    }

    public static void appendConditionString(
        @NotNull DBDDataFilter filter,
        @NotNull DBPDataSource dataSource,
        @Nullable String conditionTable,
        @NotNull StringBuilder query,
        boolean inlineCriteria)
    {
        String operator = filter.isAnyConstraint() ? " OR " : " AND ";  //$NON-NLS-1$ $NON-NLS-2$
        boolean hasWhere = false;
        for (DBDAttributeConstraint constraint : filter.getConstraints()) {
            String condition = getConstraintCondition(dataSource, constraint, inlineCriteria);
            if (condition == null) {
                continue;
            }

            if (hasWhere) query.append(operator);
            hasWhere = true;
            if (conditionTable != null) {
                query.append(conditionTable).append('.');
            }
            query.append(DBUtils.getObjectFullName(dataSource, constraint.getAttribute()));
            query.append(' ').append(condition);
        }

        if (!CommonUtils.isEmpty(filter.getWhere())) {
            if (hasWhere) query.append(operator);
            query.append(filter.getWhere());
        }
    }

    public static void appendOrderString(@NotNull DBDDataFilter filter, @NotNull DBPDataSource dataSource, @Nullable String conditionTable, @NotNull StringBuilder query)
    {
        // Construct ORDER BY
        boolean hasOrder = false;
        for (DBDAttributeConstraint co : filter.getOrderConstraints()) {
            if (hasOrder) query.append(',');
            if (conditionTable != null) {
                query.append(conditionTable).append('.');
            }
            query.append(DBUtils.getObjectFullName(co.getAttribute()));
            if (co.isOrderDescending()) {
                query.append(" DESC"); //$NON-NLS-1$
            }
            hasOrder = true;
        }
        if (!CommonUtils.isEmpty(filter.getOrder())) {
            if (hasOrder) query.append(',');
            query.append(filter.getOrder());
        }
    }

    @Nullable
    public static String getConstraintCondition(DBPDataSource dataSource, DBDAttributeConstraint constraint, boolean inlineCriteria) {
        String criteria = constraint.getCriteria();
        if (!CommonUtils.isEmpty(criteria)) {
            final char firstChar = criteria.trim().charAt(0);
            if (!Character.isLetter(firstChar) && firstChar != '=' && firstChar != '>' && firstChar != '<' && firstChar != '!') {
                return '=' + criteria;
            } else {
                return criteria;
            }
        } else if (constraint.getOperator() != null) {
            DBCLogicalOperator operator = constraint.getOperator();
            StringBuilder conString = new StringBuilder();
            if (constraint.isReverseOperator()) {
                conString.append("NOT ");
            }
            conString.append(operator.getStringValue());
            if (operator.getArgumentCount() > 0) {
                for (int i = 0; i < operator.getArgumentCount(); i++) {
                    if (i > 0) {
                        conString.append(" AND");
                    }
                    if (inlineCriteria) {
                        conString.append(' ').append(convertValueToSQL(dataSource, constraint.getAttribute(), constraint.getValue()));
                    } else {
                        conString.append(" ?");
                    }
                }
            }
            return conString.toString();
        } else {
            return null;
        }
    }

    public static String convertValueToSQL(DBPDataSource dataSource, DBSAttributeBase attribute, @Nullable Object value) {
        if (DBUtils.isNullValue(value)) {
            return SQLConstants.NULL_VALUE;
        }
        DBDValueHandler valueHandler = DBUtils.findValueHandler(dataSource, attribute);
        String strValue = valueHandler.getValueDisplayString(attribute, value, DBDDisplayFormat.NATIVE);
        SQLDialect sqlDialect = null;
        if (dataSource instanceof SQLDataSource) {
            sqlDialect = ((SQLDataSource) dataSource).getSQLDialect();
        }
        if (value instanceof Number) {
            return strValue;
        }
        switch (attribute.getDataKind()) {
            case BOOLEAN:
            case NUMERIC:
            case DATETIME:
                return strValue;
            case STRING:
            case ROWID:
                if (sqlDialect != null) {
                    strValue = sqlDialect.escapeString(strValue);
                }
                return '\'' + strValue + '\'';
            default:
                return strValue;
        }
    }

    public static String getColumnTypeModifiers(DBSAttributeBase column, String typeName, DBPDataKind dataKind) {
        if (dataKind == DBPDataKind.STRING) {
            if (typeName.indexOf('(') == -1) {
                final long maxLength = column.getMaxLength();
                if (maxLength > 0) {
                    return "(" + maxLength + ")";
                }
            }
        } else if (dataKind == DBPDataKind.NUMERIC) {
            if (typeName.equalsIgnoreCase("DECIMAL") || typeName.equalsIgnoreCase("NUMERIC") || typeName.equalsIgnoreCase("NUMBER")) {
                int scale = column.getScale();
                int precision = column.getPrecision();
                if (scale >= 0 && precision >= 0 && !(scale == 0 && precision == 0)) {
                    return "(" + precision + ',' + scale + ')';
                }
            }
        }
        return null;
    }

    public static boolean isExecQuery(@NotNull SQLDialect dialect, String query) {
        // Check for EXEC query
        final Collection<String> executeKeywords = dialect.getExecuteKeywords();
        if (!CommonUtils.isEmpty(executeKeywords)) {
            final String queryStart = getFirstKeyword(query);
            for (String keyword : executeKeywords) {
                if (keyword.equalsIgnoreCase(queryStart)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getScriptDescripion(String sql) {
        sql = stripComments(BasicSQLDialect.INSTANCE, sql);
        Matcher matcher = CREATE_PREFIX_PATTERN.matcher(sql);
        if (matcher.find() && matcher.start(0) == 0) {
            sql = sql.substring(matcher.end(1));
        }
        sql = sql.replaceAll(" +", " ");
        if (sql.length() > MAX_SQL_DESCRIPTION_LENGTH) {
            sql = sql.substring(0, MAX_SQL_DESCRIPTION_LENGTH) + " ...";
        }
        return sql;
    }

    @Nullable
    public static String getScriptDescription(@NotNull IFile sqlScript)
    {
        try {
            //log.debug("Read script '" + sqlScript.getName() + "' description");
            StringBuilder sql = new StringBuilder();
            try (BufferedReader is = new BufferedReader(new InputStreamReader(sqlScript.getContents()))) {
                for (;;) {
                    String line = is.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.startsWith("--") ||
                        line.startsWith("Rem") ||
                        line.startsWith("rem") ||
                        line.startsWith("REM")
                        )
                    {
                        continue;
                    }
                    sql.append(line).append('\n');
                    if (sql.length() > MIN_SQL_DESCRIPTION_LENGTH) {
                        break;
                    }
                }
            }
            return SQLUtils.getScriptDescripion(sql.toString());
        } catch (Exception e) {
            log.warn("", e);
        }
        return null;
    }

}