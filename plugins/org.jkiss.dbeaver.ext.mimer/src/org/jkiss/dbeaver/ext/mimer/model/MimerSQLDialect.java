/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Mimer SQL dialect. Most syntax already comes from the standard SQL:2016 baseline ({@link
 * GenericSQLDialect}'s own base), so this class only adds Mimer's own keyword/function set and
 * PSM block boundaries.
 *
 * @author Mimer Information Technology
 */
public class MimerSQLDialect extends GenericSQLDialect {

    private static final String[] EXEC_KEYWORDS = {"CALL"};

    /**
     * No {@code {"FOR", "END FOR"}} pair for the PSM {@code FOR} loop, unlike {@code IF}/{@code
     * WHILE}/{@code CASE}. Reason:
     * <ul>
     * <li>Core matches block keywords by plain text, with no context - so the {@code FOR} in the
     * new "Key Join" syntax got merged into the next statement.
     * </ul>
     */
    private static final String[][] MIMER_BEGIN_END_BLOCK = {
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"IF", SQLConstants.BLOCK_END + " IF"},
        {"WHILE", SQLConstants.BLOCK_END + " WHILE"},
        {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END + " CASE"},
    };

    public MimerSQLDialect() {
        super("Mimer SQL", "mimer");
    }

    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addSQLKeywords(Arrays.asList(
            "BUILTIN", "DATABANK", "IDENT", "SHADOW", "MODULE", "DOMAIN", "SYNONYM",
            "ENTER", "LEAVE", "REPEAT", "ITERATE", "SIGNAL", "RESIGNAL", "CONDITION",
            "CLUSTERED", "IGNORE", "INCLUDE",
            // Rest of the PSM control-flow family - ENTER/LEAVE/REPEAT/ITERATE/SIGNAL/
            // RESIGNAL/CONDITION above already covered part of it.
            "DO", "ELSEIF", "HANDLER", "LOOP", "UNTIL", "WHILE",
            "CURRENT_USER", "CURRENT_PATH", "OFFSET",
            // VALUE is reserved in Mimer SQL - see the reserved-word appendix in the Mimer SQL
            // Manual, https://docs.mimer.com/MimerSqlManual/latest. Core DBeaver deliberately
            // excludes it from its own reserved-word set (too common a column name for most
            // dialects), but Mimer SQL requires "value" to be quoted as a column name.
            "VALUE"
        ));
        addFunctions(Arrays.asList(
            "BUILTIN.UUID_AS_TEXT", "BUILTIN.GIS_LOCATION_AS_TEXT",
            "BUILTIN.GIS_LATITUDE_AS_TEXT", "BUILTIN.GIS_LONGITUDE_AS_TEXT",
            "BUILTIN.GIS_LATITUDE_AS_DOUBLE", "BUILTIN.GIS_LONGITUDE_AS_DOUBLE",
            "BUILTIN.BEGINS_WORD", "BUILTIN.MATCH_WORD", "BUILTIN.UTC_TIMESTAMP",
            "CHAR_LENGTH", "BIT_LENGTH",
            "POSITION", "OVERLAY",
            "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_PROGRAM",
            "EXTRACT", "MOD", "LN", "EXP", "CEILING",
            "WIDTH_BUCKET", "COALESCE", "NULLIF", "CAST",
            // Trigonometric / numeric
            "ACOS", "ASIN", "ATAN", "ATAN2", "COS", "COSH", "COT", "DEGREES", "RADIANS",
            "SIGN", "SIN", "SINH", "TAN", "TANH", "LOG10", "ROUND", "TRUNCATE",
            // Character / string
            "ASCII_CHAR", "ASCII_CODE", "UNICODE_CHAR", "UNICODE_CODE",
            "BEGINS", "INDEX_CHAR", "LEFT", "LOCATE", "PASTE", "REGEXP_MATCH", "REPLACE",
            "RIGHT", "SOUNDEX", "TAIL",
            // Datetime parts
            "DAY", "DAYOFMONTH", "DAYOFWEEK", "DAYOFYEAR", "HOUR", "MINUTE", "MONTH",
            "QUARTER", "SECOND", "WEEK",
            "IRAND"
        ));

        List<String> builtinTypes = new ArrayList<>(MimerConstants.TYPE_NAMES_UNKNOWN_CATALOG_KIND);
        builtinTypes.add("BUILTIN.UUID");
        addDataTypes(builtinTypes);
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return MIMER_BEGIN_END_BLOCK;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsTableDropCascade() {
        // Mimer SQL's DROP TABLE takes an optional [RESTRICT | CASCADE]; returning true here makes
        // core's SQLTableManager offer the "Cascade" checkbox on the delete-confirmation dialog.
        // MimerTableManager then routes a ticked box through MimerCascadeDropUtil for the extra
        // "are you sure" prompt.
        return true;
    }

    @Override
    public String getDualTableName() {
        return "SYSTEM.ONEROW";
    }

    // Core doesn't know these Mimer-specific rules for a column's Length/Scale suffix and
    // COLLATE clause, so this overrides it:
    //
    // - A LOB column (CHARACTER LARGE OBJECT etc.) can take an explicit length, but core skips
    //   the length for any type name containing "LOB" - which doesn't match Mimer's spelled-out
    //   form, so the length was silently dropped.
    // - Plain INTEGER can take a length too (INTEGER(10)), but core only does this for
    //   DECIMAL/NUMERIC/NUMBER/BIT.
    // - CHARACTER VARYING/NATIONAL CHARACTER VARYING require an explicit length (unlike CHAR and NCHAR,
    //   which defaults to 1) - if the Length field was left blank, fill in a default here so the
    //   create dialog can stay blank regardless of type.
    // - For a column still being created (not yet saved), always use the length actually typed
    //   in, instead of core's own logic - which can wrongly drop it if it happens to match the
    //   type's default (see the "NATIONAL CHARACTER" bug this fixed).
    // - If the column has a collation set, append COLLATE <name> after the type - skipped for a
    //   non-character column, so a leftover collation can't produce invalid SQL if the type
    //   changes.
    // - If the type name we're given already contains a COLLATE clause, do nothing further - the
    //   Data Transfer feature can pass back an already-fully-built type string, and appending
    //   anything more would land after the COLLATE clause instead of before it.
    // - TINYINT never accepts a length.
    @Override
    public String getColumnTypeModifiers(
        @NotNull DBPDataSource dataSource,
        @NotNull DBSTypedObject column,
        @NotNull String typeName,
        @NotNull DBPDataKind dataKind
    ) {
        if (containsCollateClause(typeName)) {
            // Already a complete declaration - nothing more to append, see the Javadoc above.
            return null;
        }
        if (isFixedNoModifierType(typeName)) {
            return null;
        }
        String modifiers;
        if (dataKind == DBPDataKind.CONTENT && isLargeObjectTypeName(typeName) && typeName.indexOf('(') == -1) {
            long maxLength = column.getMaxLength();
            modifiers = (maxLength > 0 && maxLength < Integer.MAX_VALUE) ? "(" + maxLength + ")" : null;
        } else if (dataKind == DBPDataKind.NUMERIC && "INTEGER".equalsIgnoreCase(typeName) && typeName.indexOf('(') == -1) {
            long maxLength = column.getMaxLength();
            modifiers = (maxLength > 0 && maxLength < Integer.MAX_VALUE) ? "(" + maxLength + ")" : null;
        } else if (dataKind == DBPDataKind.STRING && isVaryingCharacterTypeName(typeName)
            && typeName.indexOf('(') == -1 && column.getMaxLength() <= 0
        ) {
            modifiers = "(" + MimerConstants.DEFAULT_VARYING_CHARACTER_LENGTH + ")";
        } else if (MimerConstants.isCharacterType(typeName) && !isLargeObjectTypeName(typeName)
            && typeName.indexOf('(') == -1
            && column instanceof MimerTableColumn mimerColumn && !mimerColumn.isPersisted()
        ) {
            long maxLength = column.getMaxLength();
            modifiers = (maxLength > 0 && maxLength < Integer.MAX_VALUE) ? "(" + maxLength + ")" : null;
        } else {
            modifiers = super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
        }
        if (column instanceof MimerTableColumn mimerColumn && !CommonUtils.isEmpty(mimerColumn.getCollation())
            && MimerConstants.isCharacterType(typeName)
        ) {
            modifiers = CommonUtils.notEmpty(modifiers) + " COLLATE " + mimerColumn.getCollation();
        }
        return modifiers;
    }

    private static boolean isLargeObjectTypeName(@NotNull String typeName) {
        String upperTypeName = typeName.toUpperCase(Locale.ENGLISH);
        return MimerConstants.TYPE_NAMES_LARGE_OBJECT.contains(upperTypeName)
            || MimerConstants.TYPE_NAMES_LARGE_OBJECT_ABBREVIATED.contains(upperTypeName);
    }

    private static boolean isVaryingCharacterTypeName(@NotNull String typeName) {
        return MimerConstants.TYPE_NAMES_VARYING_CHARACTER_REQUIRES_LENGTH.contains(typeName.toUpperCase(Locale.ENGLISH));
    }

    private static boolean containsCollateClause(@NotNull String typeName) {
        return typeName.toUpperCase(Locale.ENGLISH).contains("COLLATE");
    }

    /**
     * Mimer type names that never take a length/precision modifier, regardless of how the type
     * is reported to us - bare {@code "TINYINT"} or schema-qualified {@code "ODBC.TINYINT"}.
     */
    private static boolean isFixedNoModifierType(@NotNull String typeName) {
        String upperTypeName = typeName.toUpperCase(Locale.ENGLISH);
        return upperTypeName.equals("TINYINT") || upperTypeName.endsWith(".TINYINT");
    }
}
