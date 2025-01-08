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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;

import java.sql.SQLException;
import java.util.Arrays;

public class CubridSQLDialect extends GenericSQLDialect
{
    public static final String CUBRID_DIALECT_ID = "cubrid";
    private static final Log log = Log.getLog(CubridSQLDialect.class);
    
    private static final String[] CUBRID_KEYWORD = {
            "BIT", "CONNECT_BY_ISCYCLE", "CONNECT_BY_ISLEAF", "CONNECT_BY_ROOT", "CURRENT_DATE", "CURRENT_DATETIME", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "DATA_TYPE",
            "DATABASE", "DATETIME", "DAY_HOUR", "DAY_MILLISECOND", "DAY_MINUTE", "DAY_SECOND", "DISTINCTROW", "DIV", "DO", "DUPLICATE",
            "HOUR_MILLISECOND", "HOUR_MINUTE", "HOUR_SECOND", "LOCAL_TRANSACTION_ID", "MILLISECOND", "MINUTE_MILLISECOND", "MINUTE_SECOND", "MODULE", "NAMES", "NCHAR",
            "ROWNUM", "SECOND_MILLISECOND", "SIBLINGS", "SQLCODE", "SQLERROR", "STATISTICS", "SYS_CONNECT_BY_PATH", "SYSDATE",
            "SYSDATETIME", "SYSTIME", "TRUNCATE", "VALUE", "XOR", "YEAR_MONTH"
    };
    
    private static final String[] REMOVE_KEYWORD = {
            "ALIAS", "ALWAYS", "ARRAY", "ASENSITIVE", "ASSIGNMENT", "ASYMMETRIC", "ATOMIC",
            "AUTHORIZATION", "BINARY", "CALLED", "CARDINALITY", "CHAIN", "CHARACTERISTICS",
            "CLUSTER", "COLLECT", "COMPLETION", "CONDITION", "CONSTRUCTOR", "CONTAINS", "CORR",
            "COVAR_POP", "COVAR_SAMP", "CUBE", "CUME_DIST", "CURSOR_NAME", "DATA_TYPE___", "DEFINED", "DEFINER",
            "DENSE_RANK", "DEREF", "DERIVED", "DETERMINISTIC", "DICTIONARY", "DISPATCH", "DYNAMIC", "ELEMENT", "END-EXEC", "EVERY",
            "EXCLUDE", "EXCLUDING", "FILTER", "FINAL", "FOLLOWING", "FREE", "FUSION", "GENERATED",
            "GRANTED", "GROUPING", "HIERARCHY", "HOLD", "IMPLEMENTATION", "INCLUDING", "INCREMENT", "INSENSITIVE", "INSTANCE", "INSTANTIABLE",
            "INVOKER", "KEY_TYPE", "LAST_DAY", "LATERAL", "LDB", "LN", "LOCATOR", "LPAD",
            "MAP", "MATCHED", "MAXVALUE", "MEMBER", "MODIFIES", 
            "MORE", "MUMPS", "NESTING", "NEW", "NOMAXVALUE", "NOMINVALUE", "NORMALIZE", "NORMALIZED",
            "NULLS", "OLD", "OPERATION", "OPERATORS", "OPTIONS", "ORDERING", "ORDINALITY",
            "OTHERS", "OVERLAY", "OVERRIDING", "PARAMETER", "PATH", "PENDANT", "PERCENTILE_CONT",
            "PERCENTILE_DISC", "PERCENT_RANK", "PLACING", "PRECEDING", "PREORDER", "PRIVATE", "PROTECTED", "PROXY", "QUALIFY",
            "READS", "REGISTER", "REGR_AVGX", "REGR_AVGY", "REGR_COUNT", "REGR_INTERCEPT", "REGR_R2", "REGR_SLOPE",
            "REGR_SXX", "REGR_SXY", "REGR_SYY", "RELEASE", "REPEATABLE", "RESTART", "RESULT", "ROW_NUMBER", "RTRIM",
            "SCALE", "SCOPE___", "SECURITY", "SELF", "SERIAL",
            "SOURCE", "SPECIFIC", "SPECIFICTYPE", "STDDEV", "STRUCTURE",
            "STYLE", "SUBMULTISET", "SYMMETRIC", "SYSTEM", "TABLESAMPLE", "TATISTICS",
            "TEST", "THERE", "TIES", "TRANSFORM",
            "TRANSFORMS", "TREAT", "TYPE", "UESCAPE", "UNBOUNDED", "UNNAMED", "UNNEST", "VARBINARY", "VARIANCE",
            "VIRTUAL", "VISIBLE", "WAIT", "WIDTH_BUCKET", "WINDOW", "WITHIN"
    };


    public CubridSQLDialect() {
        super("Cubrid", "cubrid");
    }

    @Override
    public void initDriverSettings(
            @NotNull JDBCSession session,
            @NotNull JDBCDataSource dataSource,
            @NotNull JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        CubridDataSource source = (CubridDataSource) dataSource;
        source.setSupportMultiSchema(isSupportMultiSchema(session));
        source.setEOLVersion(isEOLVersion(session));

        for(String removeKeyWord: REMOVE_KEYWORD) {
            this.removeSQLKeyword(removeKeyWord);
        }
        this.addSQLKeywords(Arrays.asList(CUBRID_KEYWORD));
    }

    @NotNull
    public boolean isSupportMultiSchema(@NotNull JDBCSession session) {
        try {
            int major = session.getMetaData().getDatabaseMajorVersion();
            int minor = session.getMetaData().getDatabaseMinorVersion();
            if (major > 11 || (major == 11 && minor >= 2)) {
                return true;
            }
        } catch (SQLException e) {
            log.error("Can't get database version", e);
        }
        return false;
    }

    @NotNull
    public boolean isEOLVersion(@NotNull JDBCSession session) {
        try {
            int major = session.getMetaData().getDatabaseMajorVersion();
            if (major <= 9) {
                return true;
            }
        } catch (SQLException e) {
            log.error("Can't get database version", e);
        }
        return false;
    }

    @NotNull
    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }
}
