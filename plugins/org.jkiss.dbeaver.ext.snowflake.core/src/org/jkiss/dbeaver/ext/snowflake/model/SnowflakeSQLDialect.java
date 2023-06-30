/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDollarQuoteRule;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLMultiWordRule;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.text.parser.*;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.List;

public class SnowflakeSQLDialect extends GenericSQLDialect implements TPRuleProvider {

    private static final String[][] SNOWFLAKE_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"IF", SQLConstants.BLOCK_END}
    };

    public SnowflakeSQLDialect() {
        super("Snowflake", "snowflake");
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addSQLKeywords(
            Arrays.asList(
                "QUALIFY",
                "ILIKE",
                "PACKAGE",
                "PIPE",
                "STAGE",
                "STREAM",
                "TAG",
                "TASK"
            ));
    }

    @Override
    public void extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull List<TPRule> rules, @NotNull RulePosition position) {
        if (position == RulePosition.INITIAL || position == RulePosition.PARTITION) {
            rules.add(new SQLDollarQuoteRule(
                position == RulePosition.PARTITION,
                false,
                false,
                dataSource == null || dataSource.getPreferenceStore().getBoolean(SnowflakeConstants.PROP_DD_STRING)
            ));
        }
        if (position == RulePosition.KEYWORDS) {
            final TPTokenDefault keywordToken = new TPTokenDefault(SQLTokenType.T_KEYWORD);
            rules.add(new SQLMultiWordRule(new String[]{"BEGIN", "TRANSACTION"}, keywordToken));
            rules.add(new SQLMultiWordRule(new String[]{"IF", "EXISTS"}, keywordToken));
            rules.add(new SQLMultiWordRule(new String[]{"IF", "NOT", "EXISTS"}, keywordToken));
        }
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return SNOWFLAKE_BEGIN_END_BLOCK;
    }

    @NotNull
    @Override
    public String getSearchStringEscape() {
        // Without escaping of wildcards Snowflake reads all metadata directly from database and ignores specified objects names
        // #9875
        return "\\";
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public String getColumnTypeModifiers(
        @NotNull DBPDataSource dataSource,
        @NotNull DBSTypedObject column,
        @NotNull String typeName,
        @NotNull DBPDataKind dataKind
    ) {
        Integer scale;
        switch (typeName) {
            case SnowflakeConstants.TYPE_NUMBER:
            case SnowflakeConstants.TYPE_NUMERIC:
            case SnowflakeConstants.TYPE_DECIMAL:
                DBSDataType dataType = DBUtils.getDataType(column);
                scale = column.getScale();
                int precision = CommonUtils.toInt(column.getPrecision());
                if (precision == 0 && dataType != null && scale != null && scale == dataType.getMinScale()) {
                    return "";
                }
                if (precision == 0 || precision > SnowflakeConstants.NUMERIC_MAX_PRECISION) {
                    precision = SnowflakeConstants.NUMERIC_MAX_PRECISION;
                }
                if (scale != null || precision > 0) {
                    // 38 - is default precision value. And we can not add scale here.
                    // It will be changed to 0 automatically after table creation from the database side.
                    return "(" + (precision > 0 ? precision : SnowflakeConstants.NUMERIC_MAX_PRECISION) +
                        (scale != null && scale > 0 ? "," + scale : "") +  ")";
                }
                break;
            case SQLConstants.DATA_TYPE_DOUBLE:
            case SnowflakeConstants.TYPE_DOUBLE_PRECISION:
            case SnowflakeConstants.TYPE_REAL:
            case SQLConstants.DATA_TYPE_FLOAT:
            case SQLConstants.DATA_TYPE_INT:
            case SnowflakeConstants.TYPE_INTEGER:
            case SQLConstants.DATA_TYPE_BIGINT:
                // These types do not have parameters
                return null;
        }
        return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
    }
    
    @Override
    public boolean validIdentifierStart(char c) {
        return SQLUtils.isLatinLetter(c) || c == '_';
    }
    
    @Override
    public boolean validIdentifierPart(char c, boolean quoted) {
        return SQLUtils.isLatinLetter(c) || Character.isDigit(c) || c == '_' || (quoted && validCharacters.indexOf(c) != -1) || c == '$';
    }

    
    @Override
    public boolean mustBeQuoted(@NotNull String str, boolean forceCaseSensitive) {
        // Unquoted object identifiers:
        // * Start with a letter (A-Z, a-z) or an underscore (“_”).
        // * Contain only letters, underscores, decimal digits (0-9), and dollar signs (“$”).
        // * Are stored and resolved as uppercase characters (e.g. id is stored and resolved as ID).
        // https://docs.snowflake.com/en/sql-reference/identifiers-syntax

        if (str.isBlank()) {
            return true;
        }
        return super.mustBeQuoted(str, forceCaseSensitive);
    }
}
