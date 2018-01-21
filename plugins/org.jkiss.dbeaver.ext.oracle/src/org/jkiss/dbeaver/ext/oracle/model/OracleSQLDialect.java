/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHex;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;

/**
 * Oracle SQL dialect
 */
class OracleSQLDialect extends JDBCSQLDialect {

    public static final String[] EXEC_KEYWORDS = new String[]{ "call" };

    public static final String[] ORACLE_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "CREATE", "ALTER", "DROP",
            "ANALYZE", "VALIDATE"}
    );

    public static final String[][] ORACLE_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"IF", SQLConstants.BLOCK_END},
        {"LOOP", SQLConstants.BLOCK_END + " LOOP"}
    };

    public static final String[] ADVANCED_KEYWORDS = {
        "PACKAGE",
        "FUNCTION",
        "TYPE",
        "TRIGGER",
        "MATERIALIZED",
        "IF",
        "EACH",
        "RETURN",
        "WRAPPED",
        "AFTER",
        "BEFORE",
        "DATABASE",
        "ANALYZE",
        "VALIDATE",
        "STRUCTURE",
        "COMPUTE",
        "STATISTICS",
        "LOOP",
        "WHILE",
        "BULK",
        "ELSIF",
        "EXIT",
    };

    public OracleSQLDialect() {
        super("Oracle");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);

        addFunctions(
            Arrays.asList(
                "SUBSTR", "APPROX_COUNT_DISTINCT",
                "REGEXP_SUBSTR", "REGEXP_INSTR", "REGEXP_REPLACE", "REGEXP_LIKE",
                // Additions from #323
                //Number Functions:
                "BITAND",
                "COSH",
                "NANVL",
                "REMAINDER",
                "SINH",
                "TANH",
                "TRUNC",

                //Character Functions Returning Character Values:
                "CHR",
                "INITCAP",
                "LPAD",
                "NLS_INITCAP",
                "NLS_LOWER",
                "NLSSORT",
                "NLS_UPPER",
                "RPAD",
                "REVERSE",
                "SUBSTRB",
                "SUBSTRC",
                "SUBSTR2",
                "SUBSTR4",

                // NLS Character Functions:
                "NLS_CHARSET_DECL_LEN",
                "NLS_CHARSET_ID",
                "NLS_CHARSET_NAME",

                //Character Functions Returning Number VALUES:
                "INSTR",
                "INSTRB",
                "INSTRC",
                "INSTR2",
                "INSTR4",
                "LENGTHB",

                //Datetime Functions:
                "ADD_MONTHS",
                "DBTIMEZONE",
                "FROM_TZ",
                "LAST_DAY",
                "MONTHS_BETWEEN",
                "NEW_TIME",
                "NEXT_DAY",
                "NUMTODSINTERVAL",
                "NUMTOYMINTERVAL",
                "SESSIONTIMEZONE",
                "SYS_EXTRACT_UTC",
                "SYSDATE",
                "SYSTIMESTAMP",
                "TO_CHAR",
                "TO_TIMESTAMP",
                "TO_TIMESTAMP_TZ",
                "TO_DSINTERVAL",
                "TO_YMINTERVAL",
                "TRUNC",
                "TZ_OFFSET",

                //General Comparison Functions:
                "GREATEST",
                "LEAST",

                //Conversion Functions:
                "ASCIISTR",
                "BIN_TO_NUM",
                "CHARTOROWID",
                "COMPOSE",
                "DECOMPOSE",
                "HEXTORAW",
                "NUMTODSINTERVAL",
                "NUMTOYMINTERVAL",
                "RAWTOHEX",
                "RAWTONHEX",
                "ROWIDTOCHAR",
                "ROWIDTONCHAR",
                "SCN_TO_TIMESTAMP",
                "TIMESTAMP_TO_SCN",
                "TO_BINARY_DOUBLE",
                "TO_BINARY_FLOAT",
                "TO_CHAR",
                "TO_CLOB",
                "TO_DATE",
                "TO_DSINTERVAL",
                "TO_LOB",
                "TO_MULTI_BYTE",
                "TO_NCHAR",
                "TO_NCLOB",
                "TO_NUMBER",
                "TO_DSINTERVAL",
                "TO_SINGLE_BYTE",
                "TO_TIMESTAMP",
                "TO_TIMESTAMP_TZ",
                "TO_YMINTERVAL",
                "TO_YMINTERVAL",
                "UNISTR",

                //Large Object Functions:
                "BFILENAME",
                "EMPTY_BLOB",
                "EMPTY_CLOB",

                //Collection Functions:
                "POWERMULTISET",
                "POWERMULTISET_BY_CARDINALITY",

                //Hierarchical FUNCTION:
                "SYS_CONNECT_BY_PATH",

                //Data Mining Functions:
                "CLUSTER_ID",
                "CLUSTER_PROBABILITY",
                "CLUSTER_SET",
                "FEATURE_ID",
                "FEATURE_SET",
                "FEATURE_VALUE",
                "PREDICTION",
                "PREDICTION_COST",
                "PREDICTION_DETAILS",
                "PREDICTION_PROBABILITY",
                "PREDICTION_SET",

                //XML Functions:
                "APPENDCHILDXML",
                "DELETEXML",
                "DEPTH",
                "EXISTSNODE",
                "EXTRACTVALUE",
                "INSERTCHILDXML",
                "INSERTXMLBEFORE",
                "PATH",
                "SYS_DBURIGEN",
                "SYS_XMLAGG",
                "SYS_XMLGEN",
                "UPDATEXML",
                "XMLAGG",
                "XMLCDATA",
                "XMLCOLATTVAL",
                "XMLCOMMENT",
                "XMLCONCAT",
                "XMLFOREST",
                "XMLPARSE",
                "XMLPI",
                "XMLQUERY",
                "XMLROOT",
                "XMLSEQUENCE",
                "XMLSERIALIZE",
                "XMLTABLE",
                "XMLTRANSFORM",

                //Encoding and Decoding Functions:
                "DECODE",
                "DUMP",
                "ORA_HASH",
                "VSIZE",

                //NULL-Related Functions:
                "LNNVL",
                "NVL",
                "NVL2",

                //Environment and Identifier Functions:
                "SYS_CONTEXT",
                "SYS_GUID",
                "SYS_TYPEID",
                "UID",
                "USERENV",

                //Aggregate Functions:
                "CORR_S",
                "CORR_K",
                "FIRST",
                "GROUP_ID",
                "GROUPING_ID",
                "LAST",
                "MEDIAN",
                "STATS_BINOMIAL_TEST",
                "STATS_CROSSTAB",
                "STATS_F_TEST",
                "STATS_KS_TEST",
                "STATS_MODE",
                "STATS_MW_TEST",
                "STATS_ONE_WAY_ANOVA",
                "STATS_T_TEST_ONE",
                "STATS_T_TEST_PAIRED",
                "STATS_T_TEST_INDEP",
                "STATS_T_TEST_INDEPU",
                "STATS_WSR_TEST",
                "STDDEV",
                "VARIANCE",

                //Analytic Functions:
                "FIRST",
                "FIRST_VALUE",
                "LAG",
                "LAST",
                "LAST_VALUE",
                "LEAD",
                "NTILE",
                "RATIO_TO_REPORT",
                "STDDEV",
                "VARIANCE",

                //Object Reference Functions:
                "MAKE_REF",
                "REFTOHEX",

                //Model Functions:
                "CV",
                "ITERATION_NUMBER",
                "PRESENTNNV",
                "PRESENTV",
                "PREVIOUS"

            ));
        removeSQLKeyword("SYSTEM");

        for (String kw : ADVANCED_KEYWORDS) {
            addSQLKeyword(kw);
        }
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return ORACLE_BEGIN_END_BLOCK;
    }

    @Override
    public String getBlockHeaderString() {
        return "DECLARE";
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsAliasInUpdate() {
        return true;
    }

    @Override
    public boolean supportsTableDropCascade() {
        return true;
    }

    @Override
    public boolean isDelimiterAfterBlock() {
        return true;
    }

    @Override
    public boolean isDelimiterAfterQuery() {
        return false;
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return BinaryFormatterHex.INSTANCE;
    }

    @Nullable
    @Override
    public String getDualTableName() {
        return "DUAL";
    }

    @NotNull
    @Override
    protected String[] getNonTransactionKeywords() {
        return ORACLE_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
        String schemaName = proc.getParentObject().getName();
        return "CALL " + schemaName + "." + proc.getName() + "(\n";
    }

    @Override
    public String getScriptDelimiter() {
        return super.getScriptDelimiter();
    }
}
