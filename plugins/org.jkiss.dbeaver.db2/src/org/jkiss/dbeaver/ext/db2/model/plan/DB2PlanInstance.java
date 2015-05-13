/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
