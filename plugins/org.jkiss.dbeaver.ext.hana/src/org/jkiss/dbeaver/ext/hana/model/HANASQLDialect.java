/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHexString;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLVariableRule;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.Locale;

public class HANASQLDialect extends GenericSQLDialect implements TPRuleProvider {

    private static final Log log = Log.getLog(HANASQLDialect.class);

    private static final String[][] HANA_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"IF", SQLConstants.BLOCK_END + " IF"},
        {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END},
        {"FOR", SQLConstants.BLOCK_END + " FOR"},
        {"WHILE", SQLConstants.BLOCK_END + " WHILE"}
    };

    @Nullable
    @Override
    public String[] getBlockHeaderStrings() {
        return new String[]{"DO"};
    }

    private static String[] HANA_FUNCTIONS = new String[]{
        "ADD_DAYS",
        "ADD_MONTH",
        "ADD_MONTHS_LAST",
        "ADD_SECONDS",
        "ADD_WORKDAYS",
        "ADD_YEARS",
        "CONCAT_NAZ",
        "CONVERT_CURRENCY",
        "CONVERT_UNIT",
        "CURRENT_CONNECTION",
        "CURRENT_DATE",
        "CURRENT_OBJECT_SCHEMA",
        "CURRENT_SCHEMA",
        "CURRENT_SITE_ID",
        "CURRENT_TIME",
        "CURRENT_TIMESTAMP",
        "CURRENT_TRANSACTION_ISOLATION_LEVEL",
        "CURRENT_USER",
        "CURRENT_USER_ID",
        "DAYS_BETWEEN",
        "ESCAPE_DOUBLE_QUOTES",
        "ESCAPE_SINGLE_QUOTES",
        "FIRST_VALUE",
        "GENERATE_PASSWORD",
        "GREATEST",
        "INITCAP",
        "JSON_QUERY",
        "JSON_TABLE",
        "JSON_VALUE",
        "LAST_DAY",
        "LAST_VALUE",
        "LEAD",
        "LEAST",
        "MONTHS_BETWEEN",
        "NEXT_DAY",
        "PLAINTEXT",
        "REPLACE_REGEXPR",
        "SECONDS_BETWEEN",
        "SERIES_GENERATE",
        "SERIES_ROUND",
        "SESSION_CONTEXT",
        "SUBARRAY",
        "SUBSTR_AFTER",
        "SUBSTR_BEFORE",
        "SUBSTRING_REGEXPR",
        "TO_BIGINT",
        "TO_BINARY",
        "TO_BLOB",
        "TO_BOOLEAN",
        "TO_CLOB",
        "TO_DATE",
        "TO_DECIMAL",
        "TO_DOUBLE",
        "TO_INT",
        "TO_INTEGER",
        "TO_JSON_BOOLEAN",
        "TO_NCLOB",
        "TO_NVARCHAR",
        "TO_REAL",
        "TO_SECONDDATE",
        "TO_SMALLDECIMAL",
        "TO_SMALLINT",
        "TO_TIME",
        "TO_TIMESTAMP",
        "TO_TINYINT",
        "TO_VARCHAR",
        "TRIM",
        "TRIM_ARRAY",
        "UNICODE",
        "WEEKDAY",
        "WORKDAYS_BETWEEN",
        "XMLEXTRACT",
        "XMLEXTRACTVALUE",
        "XMLTABLE",
        "YEARS_BETWEEN"
    };

    public HANASQLDialect() {
        super("HANA", "sap_hana");
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return HANA_BEGIN_END_BLOCK;
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addFunctions(Arrays.asList(HANA_FUNCTIONS));
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean validIdentifierStart(char c) {
        return super.validIdentifierStart(c) || c == '_';
    }
    
    /*
     * expression evaluation
     */
    @Override
    public String getDualTableName() {
        return "DUMMY";
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column,
            @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        String ucTypeName = CommonUtils.notEmpty(typeName).toUpperCase(Locale.ENGLISH);
        if (HANAConstants.DATA_TYPE_NAME_REAL_VECTOR.equals(ucTypeName)) {
            long dim = column.getMaxLength();
            if ((dim > 0) && (dim <= 65000)) {
                return "(" + Long.toString(dim) + ")";
            }
            return "";
        } else if ((HANAConstants.DATA_TYPE_NAME_ST_POINT.equals(ucTypeName)
                || HANAConstants.DATA_TYPE_NAME_ST_GEOMETRY.equals(ucTypeName))
                && (column instanceof HANATableColumn)) {
            HANATableColumn hanaColumn = (HANATableColumn) column;
            try {
                int srid = hanaColumn.getAttributeGeometrySRID(new VoidProgressMonitor());
                return "(" + Integer.toString(srid) + ")";
            } catch (DBCException e) {
                log.info("Could not determine SRID of column", e);
            }
        }
        return super.getColumnTypeModifiers(dataSource, column, ucTypeName, dataKind);
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return BinaryFormatterHexString.INSTANCE;
    }

    @NotNull
    @Override
    public String getSearchStringEscape() {
        // https://github.com/dbeaver/dbeaver/issues/9998#issuecomment-805710837
        return "\\";
    }

    @NotNull
    @Override
    public TPRule[] extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull RulePosition position) {
        if (position == RulePosition.FINAL) {
            return new TPRule[] { new SQLVariableRule(this) };
        }
        return new TPRule[0];
    }

    @Override
    public boolean isStripCommentsBeforeBlocks() {
        return true;
    }

    @Override
    public boolean mustBeQuoted(@NotNull String str, boolean forceCaseSensitive) {
        for (int i = 0; i < str.length(); i++) {
            int c = str.charAt(i);
            if (Character.isLetter(c) && !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')) {
                return true;
            }
        }
        return super.mustBeQuoted(str, forceCaseSensitive);
    }
}
