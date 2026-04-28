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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHexString;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;

import java.util.Arrays;

public class FireBirdSQLDialect extends GenericSQLDialect {

    private boolean supportsAsBeforeTableAlias = true;

    private static final String[] FB_BLOCK_HEADERS = new String[]{
        "EXECUTE BLOCK",
    };

    private static final String[][] FB_BEGIN_END_BLOCK = new String[][]{
        {"BEGIN", "END"},
    };

    private static final String[] DDL_KEYWORDS = new String[]{
        "CREATE", "ALTER", "DROP", "EXECUTE", "RECREATE", "COMMENT"
    };

    // Firebird-specific keywords not covered by JDBC metadata or the generic dialect.
    // Covers Firebird 3.0 baseline plus 4.0/5.0 additions (LATERAL, SKIP LOCKED, etc.).
    private static final String[] FIREBIRD_KEYWORDS = new String[]{
        "ACCENT",
        "AUTONOMOUS",
        "BINARY",
        "BLOCK",
        "BODY",
        "BOOLEAN",
        "BREAK",
        "COMMENT",
        "COMPUTED",
        "CONNECTIONS",
        "CONTAINING",
        "CORR",
        "COVAR_POP",
        "COVAR_SAMP",
        "CURRENT_ROLE",
        "CURRENT_USER",
        "DECFLOAT",
        "DEFINER",
        "DELETING",
        "DETERMINISTIC",
        "DOMAIN",
        "EXCEPTION",
        "FETCH",
        "FOLLOWING",
        "GDSCODE",
        "GENERATED",
        "GENERATOR",
        "IDENTITY",
        "INSERTING",
        "INT128",
        "INVOKER",
        "LATERAL",
        "LOCALTIME",
        "LOCALTIMESTAMP",
        "LOCKED",
        "MATCHED",
        "MERGE",
        "NCHAR",
        "OFFSET",
        "OPTIMIZE",
        "OVER",
        "OVERRIDING",
        "PACKAGE",
        "PARTITION",
        "PERCENT",
        "PRECEDING",
        "RANGE",
        "RECREATE",
        "REGR_AVGX",
        "REGR_AVGY",
        "REGR_COUNT",
        "REGR_INTERCEPT",
        "REGR_R2",
        "REGR_SLOPE",
        "REGR_SXX",
        "REGR_SXY",
        "REGR_SYY",
        "RETURNING",
        "ROWS",
        "SCROLL",
        "SECURITY",
        "SIMILAR",
        "SKIP",
        "SQLCODE",
        "SQLSTATE",
        "STARTING",
        "STDDEV_POP",
        "STDDEV_SAMP",
        "TIES",
        "TRANSACTION",
        "UNBOUNDED",
        "UPDATING",
        "USING",
        "VALUE",
        "VARBINARY",
        "VAR_POP",
        "VAR_SAMP",
        "WEEKDAY",
        "WINDOW",
        "YEARDAY",
    };

    // Firebird built-in scalar, aggregate, and window functions.
    // Includes functions available since Firebird 2.x through 5.0.
    private static final String[] FIREBIRD_FUNCTIONS = {
        // Math functions
        "ABS",
        "ACOS",
        "ACOSH",
        "ASIN",
        "ASINH",
        "ATAN",
        "ATAN2",
        "ATANH",
        "CEIL",
        "CEILING",
        "COS",
        "COSH",
        "EXP",
        "FLOOR",
        "LN",
        "LOG",
        "LOG10",
        "MOD",
        "PI",
        "POWER",
        "SIGN",
        "SIN",
        "SINH",
        "SQRT",
        "TAN",
        "TANH",
        "TRUNC",
        // String functions
        "ASCII_CHAR",
        "ASCII_VAL",
        "BIT_LENGTH",
        "CHAR_LENGTH",
        "CHARACTER_LENGTH",
        "HASH",
        "LEFT",
        "LOWER",
        "LPAD",
        "OCTET_LENGTH",
        "OVERLAY",
        "POSITION",
        "REPLACE",
        "REVERSE",
        "RIGHT",
        "RPAD",
        "SUBSTRING",
        "TRIM",
        "UPPER",
        // Date/time functions
        "DATEADD",
        "DATEDIFF",
        "EXTRACT",
        "FIRST_DAY",
        "LAST_DAY",
        // Conditional / type functions
        "CAST",
        "COALESCE",
        "DECODE",
        "IIF",
        "MAXVALUE",
        "MINVALUE",
        "NULLIF",
        // UUID functions
        "CHAR_TO_UUID",
        "GEN_UUID",
        "UUID_TO_CHAR",
        // Context functions
        "RDB$GET_CONTEXT",
        "RDB$SET_CONTEXT",
        // Binary / bitwise functions
        "BIN_AND",
        "BIN_NOT",
        "BIN_OR",
        "BIN_SHL",
        "BIN_SHR",
        "BIN_XOR",
        // Aggregate functions
        "LIST",
        // BLOB functions
        "BLOB_APPEND",
        // Miscellaneous
        "GEN_ID",
        "RAND",
        // Window functions (FB 3.0+)
        "ROW_NUMBER",
        "RANK",
        "DENSE_RANK",
        "LAG",
        "LEAD",
        "FIRST_VALUE",
        "LAST_VALUE",
        // Window functions (FB 4.0+)
        "CUME_DIST",
        "NTH_VALUE",
        "NTILE",
        "PERCENT_RANK",
        // Cryptographic functions (FB 4.0+)
        "BASE64_DECODE",
        "BASE64_ENCODE",
        "CRYPT_HASH",
        "DECRYPT",
        "ENCRYPT",
        "HEX_DECODE",
        "HEX_ENCODE",
        "MAKE_DBKEY",
        "RSA_DECRYPT",
        "RSA_ENCRYPT",
        "RSA_SIGN_HASH",
        "RSA_VERIFY_HASH",
        // FB 5.0 functions
        "UNICODE_CHAR",
        "UNICODE_VAL",
    };

    public FireBirdSQLDialect() {
        super("Firebird", "firebird");
    }

    @NotNull
    @Override
    public String[] getDDLKeywords() {
        return DDL_KEYWORDS;
    }

    @Override
    public String[] getBlockHeaderStrings() {
        return FB_BLOCK_HEADERS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return FB_BEGIN_END_BLOCK;
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        if (!dataSource.isServerVersionAtLeast(2, 0)) {
            supportsAsBeforeTableAlias = false;
        }
        turnFunctionIntoKeyword("TRUNCATE");
        addKeywords(Arrays.asList(FIREBIRD_KEYWORDS), DBPKeywordType.KEYWORD);
        addFunctions(Arrays.asList(FIREBIRD_FUNCTIONS));
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsAliasInHaving() {
        return false;
    }

    @Override
    public boolean supportsAsKeywordBeforeAliasInFromClause() {
        return supportsAsBeforeTableAlias;
    }

    @Override
    public boolean validIdentifierPart(char c, boolean quoted) {
        return super.validIdentifierPart(c, quoted) || c == '$';
    }

    @Override
    protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
        return "select * from " + proc.getFullyQualifiedName(DBPEvaluationContext.DML);
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return true;
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return BinaryFormatterHexString.INSTANCE;
    }
}
