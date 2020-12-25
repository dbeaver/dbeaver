/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * DB2 EXPLAIN_INSTANCE table
 *
 * @author Denis Forveille
 *
 */

/**
 * DB2 EXPLAIN_INSTANCE table
 *
 * @author Denis Forveille
 */
public class DB2PlanInstance {

    // DF : This class is not used yet by the tool

    private static String SEL_EXP_STATEMENT;

    static {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("SELECT *");
        sb.append(" FROM EXPLAIN_STATEMENT");
        sb.append(" WHERE EXPLAIN_REQUESTER = ?");
        sb.append("   AND EXPLAIN_TIME = ?");
        sb.append("   AND SOURCE_NAME = ?");
        sb.append("   AND SOURCE_SCHEMA = ?");
        sb.append("   AND SOURCE_VERSION = ?");
        sb.append(" WITH UR");
        SEL_EXP_STATEMENT = sb.toString();
    }

    private DB2PlanStatement db2PlanStatement;

    private String statement_id;

    private String explainRequester;
    private Timestamp explainTime;
    private String sourceName;
    private String sourceSchema;
    private String sourceVersion;

    // ------------
    // Constructors
    // ------------

    public DB2PlanInstance(DB2DataSource dataSource,
                           JDBCSession session,
                           ResultSet dbResult,
                           DB2PlanStatement db2PlanStatement) throws SQLException
    {

        this.db2PlanStatement = db2PlanStatement;

        this.explainRequester = JDBCUtils.safeGetStringTrimmed(dbResult, "EXPLAIN_REQUESTER");
        this.explainTime = JDBCUtils.safeGetTimestamp(dbResult, "EXPLAIN_TIME");
        this.sourceName = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCE_NAME");
        this.sourceSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCE_SCHEMA");
        this.sourceVersion = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCE_VERSION");

    }

    // -------------
    // Standards Getters
    // -------------
    public String getStatement_id()
    {
        return statement_id;
    }

    public String getExplainRequester()
    {
        return explainRequester;
    }

    public Timestamp getExplainTime()
    {
        return explainTime;
    }

    public String getSourceName()
    {
        return sourceName;
    }

    public String getSourceSchema()
    {
        return sourceSchema;
    }

    public String getSourceVersion()
    {
        return sourceVersion;
    }

    // -------
    // Queries
    // -------

}
