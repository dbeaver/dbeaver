/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.info.DB2Parameter;
import org.jkiss.dbeaver.ext.db2.info.DB2XMLString;
import org.jkiss.dbeaver.ext.db2.model.DB2Bufferpool;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2MaterializedQueryTable;
import org.jkiss.dbeaver.ext.db2.model.DB2Package;
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Sequence;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.ext.db2.model.DB2Trigger;
import org.jkiss.dbeaver.ext.db2.model.DB2View;
import org.jkiss.dbeaver.ext.db2.model.DB2XMLSchema;
import org.jkiss.dbeaver.ext.db2.model.app.DB2ServerApplication;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablespaceDataType;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2Nickname;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DB2 Utils
 * 
 * @author Denis Forveille
 */
public class DB2Utils {

    private static final Log LOG = LogFactory.getLog(DB2Utils.class);

    private static final String LINE_SEP = "\n";

    // TODO DF: many things in this class could probably be factorized or genreric-ified

    // DB2LOOK
    private static final String CALL_DB2LK_GEN = "CALL SYSPROC.DB2LK_GENERATE_DDL(?,?)";
    private static final String CALL_DB2LK_CLEAN = "CALL SYSPROC.DB2LK_CLEAN_TABLE(?)";
    private static final String SEL_DB2LK = "SELECT SQL_STMT FROM SYSTOOLS.DB2LOOK_INFO_V WHERE OP_TOKEN = ? ORDER BY OP_SEQUENCE WITH UR";
    private static final String DB2LK_COMMAND = "-e -td %s -t %s";

    // EXPLAIN
    private static final String CALL_INST_OBJ = "CALL SYSPROC.SYSINSTALLOBJECTS(?,?,?,?)";
    private static final int CALL_INST_OBJ_BAD_RC = -438;
    private static final String SEL_LIST_TS;

    static {
        StringBuilder sb = new StringBuilder(512);
        sb.append("SELECT TA.TBSPACE");
        sb.append("  FROM SYSCAT.TBSPACEAUTH TA");
        sb.append("     , SYSCAT.TABLESPACES T");
        sb.append(" WHERE TA.GRANTEE IN ('PUBLIC',SESSION_USER)");
        sb.append("   AND TA.USEAUTH In ('Y','G')");
        sb.append("   AND T.TBSPACE = TA.TBSPACE");
        sb.append("   AND T.DATATYPE IN (");
        sb.append("                     '").append(DB2TablespaceDataType.A.name()).append("'");
        sb.append("                    ,'").append(DB2TablespaceDataType.L.name()).append("'");
        sb.append("                     )");
        sb.append(" ORDER BY TA.TBSPACE");
        sb.append(" WITH UR");
        SEL_LIST_TS = sb.toString();
    }

    // ADMIN ACTIONS
    private static final String CALL_ADS = "CALL SYSPROC.ADMIN_DROP_SCHEMA(?,NULL,?,?)";

    // DBCFG/DBMCFG/XMLStrings
    private static final String SEL_DBCFG = "SELECT * FROM SYSIBMADM.DBCFG ORDER BY NAME  WITH UR";
    private static final String SEL_DBMCFG = "SELECT * FROM SYSIBMADM.DBMCFG ORDER BY NAME WITH UR";
    private static final String SEL_XMLSTRINGS = "SELECT * FROM SYSCAT.XMLSTRINGS ORDER BY STRINGID WITH UR";

    // APPLICATIONS
    private static final String SEL_APP = "SELECT * FROM SYSIBMADM.APPLICATIONS WITH UR";
    private static final String FORCE_APP = "CALL SYSPROC.ADMIN_CMD( 'force application (%d)')";
    private static final String AUT_APP;
    static {
        StringBuilder sb = new StringBuilder(512);
        sb.append("SELECT 1");
        sb.append("  FROM TABLE (SYSPROC.AUTH_LIST_AUTHORITIES_FOR_AUTHID (?, 'U')) AS T ");
        sb.append(" WHERE AUTHORITY IN ('SYSMON','SYSMAINT','SYSADM','SYSCTRL')");
        sb.append("   AND 'Y' in (D_USER,D_GROUP,D_PUBLIC,ROLE_USER,ROLE_GROUP,ROLE_PUBLIC,D_ROLE)");
        sb.append(" WITH UR");
        AUT_APP = sb.toString();
    }

    // ------------------------
    // Check for Authorisations
    // ------------------------
    public static Boolean userIsAuthorisedForAPPLICATIONS(JDBCSession session, String authId) throws SQLException
    {
        LOG.debug("Check if user '" + authId + "' is authorised for SYSIBMADM Views");
        String res = JDBCUtils.queryString(session, AUT_APP, authId);
        if (res == null) {
            return false;
        }

        // TODO DF: Incomplete need to check thistoo:

        // For all administrative views in the SYSIBMADM schema, you need SELECT privilege on the view. This can be validated
        // with the following query to check that your authorization ID, or a group or a role to which you belong, has SELECT
        // privilege (that is, it meets the search criteria and is listed in the GRANTEE column):
        //
        // SELECT GRANTEE, GRANTEETYPE
        // FROM SYSCAT.TABAUTH
        // WHERE TABSCHEMA = 'SYSIBMADM' AND TABNAME = '<view_name>' AND
        // SELECTAUTH <> 'N'
        //
        // where <view_name> is the name of the administrative view.
        // With the exception of SYSIBMADM.AUTHORIZATIONIDS, SYSIBMADM.OBJECTOWNERS, and SYSIBMADM.PRIVILEGES, you also need
        // EXECUTE privilege on the underlying administrative table function. The underlying administrative table function is
        // listed in the authorization section of the administrative view. This can be validated with the following query:
        //
        // SELECT GRANTEE, GRANTEETYPE
        // FROM SYSCAT.ROUTINEAUTH
        // WHERE SCHEMA = 'SYSPROC' AND SPECIFICNAME = '<routine_name>' AND
        // EXECUTEAUTH <> 'N'
        return true;
    }

    public static Boolean callAdminDropSchema(DBRProgressMonitor monitor, DB2DataSource dataSource, String schemaName,
        String errorSchemaName, String errorTableName) throws SQLException
    {
        LOG.debug("Call admin_drop_schema for " + schemaName);
        JDBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.META, "ADMIN_DROP_SCHEMA");
        try {

            JDBCCallableStatement stmtSP = session.prepareCall(CALL_ADS);
            try {
                stmtSP.setString(1, schemaName);
                stmtSP.setString(2, errorSchemaName);
                stmtSP.setString(3, errorTableName);
                return stmtSP.execute();
            } finally {
                stmtSP.close();
            }
        } finally {
            session.close();
        }
    }

    // ------------------------
    // Generate DDL
    // ------------------------

    // TODO DF: Tables in SYSTOOLS tables must exist first
    public static String generateDDLforTable(DBRProgressMonitor monitor, String statementDelimiter, DB2DataSource dataSource,
        DB2Table db2Table) throws DBException
    {
        LOG.debug("Generate DDL for " + db2Table.getFullQualifiedName());

        monitor.beginTask("Generating DDL", 1);

        // DF: Use "Undocumented" SYSPROC.DB2LK_GENERATE_DDL stored proc
        // Ref to db2look :
        // http://pic.dhe.ibm.com/infocenter/db2luw/v10r5/topic/com.ibm.db2.luw.admin.cmd.doc/doc/r0002051.html
        //
        // Option that do not seem to work: -dp -a ...

        Integer token = 0;
        StringBuilder sb = new StringBuilder(2048);
        String command = String.format(DB2LK_COMMAND, statementDelimiter, db2Table.getFullQualifiedName());

        JDBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.META, "Generate DDL");
        try {
            LOG.debug("Calling DB2LK_GENERATE_DDL with command : " + command);

            JDBCCallableStatement stmtSP = session.prepareCall(CALL_DB2LK_GEN);
            try {
                stmtSP.registerOutParameter(2, java.sql.Types.INTEGER);
                stmtSP.setString(1, command);
                stmtSP.executeUpdate();
                token = stmtSP.getInt(2);
            } finally {
                stmtSP.close();
            }

            LOG.debug("Token = " + token);

            // Read result
            JDBCPreparedStatement stmtSel = session.prepareStatement(SEL_DB2LK);
            try {
                stmtSel.setInt(1, token);
                JDBCResultSet dbResult = stmtSel.executeQuery();
                try {
                    Clob ddlStmt;
                    Long ddlLength;
                    Long ddlStart = 1L;
                    while (dbResult.next()) {
                        ddlStmt = dbResult.getClob(1);
                        ddlLength = ddlStmt.length() + 1L;
                        sb.append(ddlStmt.getSubString(ddlStart, ddlLength.intValue()));
                        sb.append(LINE_SEP);
                        ddlStmt.free();
                    }
                } finally {
                    dbResult.close();
                }
            } finally {
                stmtSel.close();
            }

            // Clean
            JDBCCallableStatement stmtSPClean = session.prepareCall(CALL_DB2LK_CLEAN);
            try {
                stmtSPClean.setInt(1, token);
                stmtSPClean.executeUpdate();
            } finally {
                stmtSPClean.close();
            }

            LOG.debug("Terminated OK");

            return sb.toString();

        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            session.close();

            monitor.done();
        }
    }

    // ------------------------
    // EXPLAIN
    // ------------------------

    public static List<String> getListOfUsableTsForExplain(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("Get List Of Usable Tablespaces For Explain Tables");

        List<String> listTablespaces = new ArrayList<String>();
        JDBCPreparedStatement dbStat = session.prepareStatement(SEL_LIST_TS);
        try {
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (dbResult.next()) {
                    listTablespaces.add(dbResult.getString(1));
                }
            } finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }

        return listTablespaces;
    }

    public static Boolean checkExplainTables(DBRProgressMonitor monitor, DB2DataSource dataSource, String explainTableSchemaName)
        throws DBCException
    {
        LOG.debug("Check EXPLAIN tables in '" + explainTableSchemaName + "'");

        monitor.beginTask("Check EXPLAIN tables", 1);

        JDBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.META, "Verify EXPLAIN tables");

        try {
            // First Check with given schema
            JDBCCallableStatement stmtSP = session.prepareCall(CALL_INST_OBJ);
            try {
                stmtSP.setString(1, "EXPLAIN"); // EXPLAIN
                stmtSP.setString(2, "V"); // Verify
                stmtSP.setString(3, ""); // Tablespace
                stmtSP.setString(4, explainTableSchemaName); // Schema
                stmtSP.execute();
                LOG.debug("EXPLAIN tables with schema " + explainTableSchemaName + " found.");

                return true;
            } catch (SQLException e) {
                LOG.debug("RC:" + e.getErrorCode() + " SQLState:" + e.getSQLState() + " " + e.getMessage());
                if (e.getErrorCode() == CALL_INST_OBJ_BAD_RC) {
                    LOG.debug("No valid EXPLAIN tables found in schema '" + explainTableSchemaName + "'.");
                    return false;
                }
                throw new DBCException(e);
            } finally {
                stmtSP.close();
            }
        } catch (SQLException e1) {
            throw new DBCException(e1);
        } finally {
            session.close();
            monitor.done();
        }
    }

    public static void createExplainTables(DBRProgressMonitor monitor, DB2DataSource dataSource, String explainTableSchemaName,
        String tablespaceName) throws DBCException
    {
        LOG.debug("Create EXPLAIN tables in " + explainTableSchemaName);

        monitor.beginTask("Create EXPLAIN Tables", 1);

        JDBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.META, "Create EXPLAIN tables");
        try {
            JDBCCallableStatement stmtSP = session.prepareCall(CALL_INST_OBJ);
            try {
                stmtSP.setString(1, "EXPLAIN"); // EXPLAIN
                stmtSP.setString(2, "C"); // Create
                stmtSP.setString(3, tablespaceName); // Tablespace
                stmtSP.setString(4, explainTableSchemaName); // Schema
                stmtSP.executeUpdate();

                LOG.debug("Creation EXPLAIN Tables : OK");
            } catch (SQLException e) {
                LOG.error("SQLException occured during EXPLAIN tables creation in schema " + explainTableSchemaName, e);
                throw new DBCException(e);
            } finally {
                stmtSP.close();
            }
        } catch (SQLException e1) {
            throw new DBCException(e1);
        } finally {
            session.close();
            monitor.done();
        }
    }

    // ---------------------
    // DBA Data and Actions
    // ---------------------

    public static Boolean forceApplication(DBRProgressMonitor monitor, DB2DataSource dataSource, Long agentId) throws SQLException
    {
        LOG.debug("Force Application : " + agentId.toString());

        JDBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.META, "Force Application");
        try {
            JDBCCallableStatement stmtSP = session.prepareCall(String.format(FORCE_APP, agentId));
            try {
                return stmtSP.execute();
            } finally {
                stmtSP.close();
            }
        } finally {
            session.close();
        }
    }

    public static List<DB2ServerApplication> readApplications(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("readApplications");

        List<DB2ServerApplication> listApplications = new ArrayList<DB2ServerApplication>();
        JDBCPreparedStatement dbStat = session.prepareStatement(SEL_APP);
        try {
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (dbResult.next()) {
                    listApplications.add(new DB2ServerApplication(dbResult));
                }
            } finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
        return listApplications;
    }

    public static List<DB2Parameter> readDBCfg(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("readDBCfg");

        List<DB2Parameter> listDBParameters = new ArrayList<DB2Parameter>();
        JDBCPreparedStatement dbStat = session.prepareStatement(SEL_DBCFG);
        try {
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (dbResult.next()) {
                    listDBParameters.add(new DB2Parameter((DB2DataSource) session.getDataSource(), dbResult));
                }
            } finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
        return listDBParameters;
    }

    public static List<DB2Parameter> readDBMCfg(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("readDBMCfg");

        List<DB2Parameter> listDBMParameters = new ArrayList<DB2Parameter>();
        JDBCPreparedStatement dbStat = session.prepareStatement(SEL_DBMCFG);
        try {
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (dbResult.next()) {
                    listDBMParameters.add(new DB2Parameter((DB2DataSource) session.getDataSource(), dbResult));
                }
            } finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
        return listDBMParameters;
    }

    public static List<DB2XMLString> readXMLStrings(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("readXMLStrings");

        List<DB2XMLString> listXMLStrings = new ArrayList<DB2XMLString>();
        JDBCPreparedStatement dbStat = session.prepareStatement(SEL_XMLSTRINGS);
        try {
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (dbResult.next()) {
                    listXMLStrings.add(new DB2XMLString((DB2DataSource) session.getDataSource(), dbResult));
                }
            } finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
        return listXMLStrings;
    }

    // ---------------
    // Objects Finders
    // ---------------

    public static DB2Tablespace findTablespaceById(DBRProgressMonitor monitor, DB2DataSource db2DataSource, Integer tablespaceId)
        throws DBException
    {
        if (db2DataSource == null) {
            return null;
        }
        for (DB2Tablespace db2Tablespace : db2DataSource.getTablespaces(monitor)) {
            if (db2Tablespace.getTbspaceId() == tablespaceId) {
                return db2Tablespace;
            }
        }
        return null;
    }

    public static DB2Bufferpool findBufferpoolById(DBRProgressMonitor monitor, DB2DataSource db2DataSource, Integer bufferpoolId)
        throws DBException
    {
        if (db2DataSource == null) {
            return null;
        }
        for (DB2Bufferpool db2Bufferpool : db2DataSource.getBufferpools(monitor)) {
            if (db2Bufferpool.getId() == bufferpoolId) {
                return db2Bufferpool;
            }
        }
        return null;
    }

    public static DB2TableColumn findColumnxBySchemaNameAndTableNameAndname(DBRProgressMonitor monitor,
        DB2DataSource db2DataSource, String db2SchemaName, String db2TableName, String db2ColumnName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        DB2Table db2Table = db2Schema.getTable(monitor, db2TableName);
        if (db2Table == null) {
            return null;
        }
        return db2Table.getAttribute(monitor, db2ColumnName);
    }

    public static DB2Index findIndexBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2IndexName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getIndex(monitor, db2IndexName);
    }

    public static DB2Module findModuleBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2ModuleName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getModule(monitor, db2ModuleName);
    }

    public static DB2Nickname findNicknameBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2NicknameName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getNickname(monitor, db2NicknameName);
    }

    public static DB2Package findPackageBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2PackageName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getPackage(monitor, db2PackageName);
    }

    public static DB2Routine findMethodBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2MethodName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getMethod(monitor, db2MethodName);
    }

    public static DB2Routine findProcedureBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2ProcedureName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getProcedure(monitor, db2ProcedureName);
    }

    public static DB2Sequence findSequenceBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2SequenceName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getSequence(monitor, db2SequenceName);
    }

    public static DB2Table findTableBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2TableName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getTable(monitor, db2TableName);
    }

    public static DB2Trigger findTriggerBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2TriggerName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getTrigger(monitor, db2TriggerName);
    }

    public static DB2Routine findUDFBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2FunctionName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getUDF(monitor, db2FunctionName);
    }

    public static DB2View findViewBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2ViewName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getView(monitor, db2ViewName);
    }

    public static DB2MaterializedQueryTable findMQTBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2MQTName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getMaterializedQueryTable(monitor, db2MQTName);
    }

    public static DB2XMLSchema findXMLSchemaByById(DBRProgressMonitor monitor, DB2DataSource db2DataSource, Long xmlSchemaId)
        throws DBException
    {
        if (db2DataSource == null) {
            return null;
        }
        // We have to iterate all Schemas...
        for (DB2Schema db2Schema : db2DataSource.getSchemas(monitor)) {
            for (DB2XMLSchema db2XMLSchema : db2Schema.getXMLSchemas(monitor)) {
                if (db2XMLSchema.getId().equals(xmlSchemaId)) {
                    return db2XMLSchema;
                }
            }
        }
        return null;
    }

    private DB2Utils()
    {
        // Pure utility class, no instanciation allowed
    }

}
