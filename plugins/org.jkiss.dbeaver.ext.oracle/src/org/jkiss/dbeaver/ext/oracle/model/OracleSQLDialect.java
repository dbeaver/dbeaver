/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHex;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;

import java.util.Arrays;

/**
 * Oracle SQL dialect
 */
class OracleSQLDialect extends JDBCSQLDialect {

    public static final String[] EXEC_KEYWORDS = new String[]{ "call" };

    public static final String[][] ORACLE_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"IF", SQLConstants.BLOCK_END}
    };

    public OracleSQLDialect(JDBCDatabaseMetaData metaData) {
        super("Oracle", metaData);
        addSQLKeyword("ANALYZE");
        addSQLKeyword("VALIDATE");
        addSQLKeyword("STRUCTURE");
        addSQLKeyword("COMPUTE");
        addSQLKeyword("STATISTICS");
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

                // NLS Character Functions:
                "NLS_CHARSET_DECL_LEN",
                "NLS_CHARSET_ID",
                "NLS_CHARSET_NAME",

                //Character Functions Returning Number VALUES:
                "INSTR",

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
    public boolean isDelimiterAfterBlock() {
        return true;
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
}
