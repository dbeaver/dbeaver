/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Utils
 * 
 * @author Denis Forveille
 */
public class DB2Utils {

    private static final Log LOG = Log.getLog(DB2Utils.class);

    private static final String LINE_SEP = ";\n";

    // TODO DF: many things in this class could probably be factorized or generic-field

    // DB2LOOK
    private static final String CALL_DB2LK_GEN = "CALL SYSPROC.DB2LK_GENERATE_DDL(?,?)";
    private static final String CALL_DB2LK_CLEAN = "CALL SYSPROC.DB2LK_CLEAN_TABLE(?)";
    private static final String SEL_DB2LK = "SELECT SQL_STMT FROM SYSTOOLS.DB2LOOK_INFO WHERE OP_TOKEN = ? ORDER BY OP_SEQUENCE WITH UR";
    private static final String DB2LK_COMMAND = "-e -x -xd -td %s -t %s";

    // EXPLAIN
    private static final String CALL_INST_OBJ = "CALL SYSPROC.SYSINSTALLOBJECTS(?,?,?,?)";
    private static final int CALL_INST_OBJ_BAD_RC = -438;
    private static final String SEL_LIST_TS_EXPLAIN;

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
        SEL_LIST_TS_EXPLAIN = sb.toString();
    }

    // ADMIN ACTIONS
    private static final String CALL_ADMIN_CMD = "CALL SYSPROC.ADMIN_CMD('%s')";

    // DBCFG/DBMCFG/XMLStrings
    private static final String SEL_DBCFG = "SELECT * FROM SYSIBMADM.DBCFG ORDER BY NAME  WITH UR";
    private static final String SEL_DBMCFG = "SELECT * FROM SYSIBMADM.DBMCFG ORDER BY NAME WITH UR";
    private static final String SEL_XMLSTRINGS = "SELECT * FROM SYSCAT.XMLSTRINGS ORDER BY STRINGID WITH UR";

    // APPLICATIONS
    private static final String SEL_APP = "SELECT * FROM SYSIBMADM.APPLICATIONS WITH UR";

    private static final String GET_MSG = "VALUES (SYSPROC.SQLERRM(?))";

    // ------------------------
    // Admin Command
    // ------------------------
    public static void callAdminCmd(DBRProgressMonitor monitor, DB2DataSource dataSource, String command) throws SQLException
    {
        LOG.debug("Call admin_cmd with '" + command + "'");
        String sql = String.format(CALL_ADMIN_CMD, command);

        monitor.beginTask("Executing command " + command, 1);

        try (JDBCSession session = DBUtils.openUtilSession(monitor, dataSource, "ADMIN_CMD")) {
            JDBCUtils.executeProcedure(session, sql);
        } finally {
            monitor.done();
        }
    }

    // ------------------------
    // Generate DDL
    // ------------------------

    // DF: Use "Undocumented" SYSPROC.DB2LK_GENERATE_DDL stored proc
    // Ref to db2look :
    // http://pic.dhe.ibm.com/infocenter/db2luw/v10r5/topic/com.ibm.db2.luw.admin.cmd.doc/doc/r0002051.html
    //
    // Options of db2look that do not seem to work: -dp . "-a" seems to work on v10.1+, "-l" seems OK in all versions
    //
    // TODO DF: Tables in SYSTOOLS tables must exist first
    public static String generateDDLforTable(DBRProgressMonitor monitor, String statementDelimiter, DB2DataSource dataSource,
        DB2Table db2Table) throws DBException
    {
        LOG.debug("Generate DDL for " + db2Table.getFullQualifiedName());

        // The DB2LK_GENERATE_DDL SP does not generate DDL for System Tables for some reason...
        // As a workaround, display a message to the end-user
        if (db2Table.getSchema().isSystem()) {
            return DB2Messages.no_ddl_for_system_tables;
        }

        // The DB2LK_GENERATE_DDL SP does not work when the name contains a space, even after trying to apply what is said in the
        // doc:
        // As a workaround, display a message to the end-user
        // Enclose multiword table names with the backslash and two sets of double quotation marks (for example, "\"My Table\"")
        // to prevent the pairing from being evaluated word-by-word by the command line processor (CLP).
        // If you use only one set of double quotation marks (for example, "My Table"), all words are converted into uppercase,
        // and the db2look command looks for an uppercase table name (for example, MY TABLE).
        if (db2Table.getFullQualifiedName().contains(" ")) {
            return DB2Messages.no_ddl_for_spaces_in_name;
        }

        monitor.beginTask("Generating DDL", 3);

        int token;
        StringBuilder sb = new StringBuilder(2048);
        String command = String.format(DB2LK_COMMAND, statementDelimiter, db2Table.getFullQualifiedName());

        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Generate DDL")) {
            LOG.debug("Calling DB2LK_GENERATE_DDL with command : " + command);

            try (JDBCCallableStatement stmtSP = session.prepareCall(CALL_DB2LK_GEN)) {
                stmtSP.registerOutParameter(2, java.sql.Types.INTEGER);
                stmtSP.setString(1, command);
                stmtSP.executeUpdate();
                token = stmtSP.getInt(2);
            }

            LOG.debug("Token = " + token);
            monitor.worked(1);

            // Read result
            try (JDBCPreparedStatement stmtSel = session.prepareStatement(SEL_DB2LK)) {
                stmtSel.setInt(1, token);
                try (JDBCResultSet dbResult = stmtSel.executeQuery()) {
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
                }
            }

            monitor.worked(2);

            // Clean
            try (JDBCCallableStatement stmtSPClean = session.prepareCall(CALL_DB2LK_CLEAN)) {
                stmtSPClean.setInt(1, token);
                stmtSPClean.executeUpdate();
            }

            monitor.worked(3);
            LOG.debug("Terminated OK");

            return sb.toString();

        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        } finally {
            monitor.done();
        }
    }

    // ------------------------
    // Error Message
    // ------------------------

    public static String getMessageFromCode(DB2DataSource db2DataSource, Integer sqlErrorCode) throws SQLException
    {
        try (JDBCSession session = DBUtils.openUtilSession(VoidProgressMonitor.INSTANCE, db2DataSource, "Get Error Code")) {
            return JDBCUtils.queryString(session, GET_MSG, sqlErrorCode);
        }
    }

    // ------------------------
    // EXPLAIN
    // ------------------------

    public static List<String> getListOfUsableTsForExplain(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("Get List Of Usable Tablespaces For Explain Tables");

        List<String> listTablespaces = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(SEL_LIST_TS_EXPLAIN)) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    listTablespaces.add(dbResult.getString(1));
                }
            }
        }

        return listTablespaces;
    }

    public static Boolean checkExplainTables(DBRProgressMonitor monitor, DB2DataSource dataSource, String explainTableSchemaName)
        throws DBCException
    {
        LOG.debug("Check EXPLAIN tables in '" + explainTableSchemaName + "'");

        monitor.beginTask("Check EXPLAIN tables", 1);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Verify EXPLAIN tables")) {
            // First Check with given schema
            try (JDBCCallableStatement stmtSP = session.prepareCall(CALL_INST_OBJ)) {
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
                throw new DBCException(e, dataSource);
            }
        } finally {
            monitor.done();
        }
    }

    public static void createExplainTables(DBRProgressMonitor monitor, DB2DataSource dataSource, String explainTableSchemaName,
        String tablespaceName) throws DBCException
    {
        LOG.debug("Create EXPLAIN tables in " + explainTableSchemaName);

        monitor.beginTask("Create EXPLAIN Tables", 1);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Create EXPLAIN tables")) {
            try (JDBCCallableStatement stmtSP = session.prepareCall(CALL_INST_OBJ)) {
                stmtSP.setString(1, "EXPLAIN"); // EXPLAIN
                stmtSP.setString(2, "C"); // Create
                stmtSP.setString(3, tablespaceName); // Tablespace
                stmtSP.setString(4, explainTableSchemaName); // Schema
                stmtSP.executeUpdate();

                LOG.debug("Creation EXPLAIN Tables : OK");
            } catch (SQLException e) {
                LOG.error("SQLException occured during EXPLAIN tables creation in schema " + explainTableSchemaName, e);
                throw new DBCException(e, dataSource);
            }
        } finally {
            monitor.done();
        }
    }

    // ---------------------
    // DBA Data and Actions
    // ---------------------

    public static List<DB2ServerApplication> readApplications(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("readApplications");

        List<DB2ServerApplication> listApplications = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(SEL_APP)) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    listApplications.add(new DB2ServerApplication(dbResult));
                }
            }
        }
        return listApplications;
    }

    public static List<DB2Parameter> readDBCfg(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("readDBCfg");

        List<DB2Parameter> listDBParameters = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(SEL_DBCFG)) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    listDBParameters.add(new DB2Parameter((DB2DataSource) session.getDataSource(), dbResult));
                }
            }
        }
        return listDBParameters;
    }

    public static List<DB2Parameter> readDBMCfg(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("readDBMCfg");

        List<DB2Parameter> listDBMParameters = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(SEL_DBMCFG)) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    listDBMParameters.add(new DB2Parameter((DB2DataSource) session.getDataSource(), dbResult));
                }
            }
        }
        return listDBMParameters;
    }

    public static List<DB2XMLString> readXMLStrings(DBRProgressMonitor monitor, JDBCSession session) throws SQLException
    {
        LOG.debug("readXMLStrings");

        List<DB2XMLString> listXMLStrings = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(SEL_XMLSTRINGS)) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    listXMLStrings.add(new DB2XMLString((DB2DataSource) session.getDataSource(), dbResult));
                }
            }
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
            if (CommonUtils.equalObjects(db2Tablespace.getTbspaceId(), tablespaceId)) {
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
            if (CommonUtils.equalObjects(db2Bufferpool.getId(), bufferpoolId)) {
                return db2Bufferpool;
            }
        }
        return null;
    }

    public static DB2TableColumn findColumnBySchemaNameAndTableNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource,
        String db2SchemaName, String db2TableName, String db2ColumnName) throws DBException
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

    public static DB2MaterializedQueryTable findMaterializedQueryTableBySchemaNameAndName(DBRProgressMonitor monitor,
        DB2DataSource db2DataSource, String db2SchemaName, String db2NicknameName) throws DBException
    {
        DB2Schema db2Schema = db2DataSource.getSchema(monitor, db2SchemaName);
        if (db2Schema == null) {
            return null;
        }
        return db2Schema.getMaterializedQueryTable(monitor, db2NicknameName);
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

    public static DB2View findViewBySchemaNameAndName(DBRProgressMonitor monitor, DB2DataSource db2DataSource, String db2SchemaName,
        String db2ViewName) throws DBException
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

    public static String formatSQLProcedureDDL(DB2DataSource db2DataSource, String rawText)
    {

        // First let the defaut SQL formater operate
        String result = SQLUtils.formatSQL(db2DataSource, rawText);

        // Put some kwywords on the same line
        result = result.replace("CREATE\r\n    PROCEDURE", "CREATE PROCEDURE");
        result = result.replace("\r\nOR REPLACE", " OR REPLACE");

        // Put each definition keywords on one line
        result = result.replace(" LANGUAGE ", "\r\nLANGUAGE ");
        result = result.replace(" SPECIFIC ", "\r\nSPECIFIC ");
        result = result.replace(" DYNAMIC RESULT ", "\r\nDYNAMIC RESULT ");
        result = result.replace(" MODIFIES SQL ", "\r\nMODIFIES SQL ");
        result = result.replace(" CONTAINS SQL ", "\r\nCONTAINS SQL ");
        result = result.replace(" READS SQL DATA ", "\r\nREADS SQL DATA ");
        result = result.replace(" NOT DETERMINISTIC ", "\r\nNOT DETERMINISTIC ");
        result = result.replace(" DETERMINISTIC ", "\r\nDETERMINISTIC ");
        result = result.replace(" CALLED ON NULL INPUT ", "\r\nCALLED ON NULL INPUT ");
        result = result.replace(" COMMIT ON RETURN ", "\r\nCOMMIT ON RETURN ");
        result = result.replace(" AUTONOMOUS ", "\r\nAUTONOMOUS ");
        result = result.replace(" INHERIT SPECIAL ", "\r\nINHERIT SPECIAL ");
        result = result.replace(" OLD SAVEPOINT ", "\r\nOLD SAVEPOINT ");
        result = result.replace(" NEW SAVEPOINT ", "\r\nNEW SAVEPOINT ");
        result = result.replace(" EXTERNAL ACTION ", "\r\nEXTERNAL ACTION ");
        result = result.replace(" NO EXTERNAL ", "\r\nNO EXTERNAL ");
        result = result.replace(" PARAMETER CCSID ", "\r\nPARAMETER CCSID ");
        result = result.replace(" BEGIN ", "\r\nBEGIN\r\n");

        // Put a CR after ";"
        result = result.replaceAll(";", ";\r\n");

        // Suppress the CRs before ";"
        result = result.replaceAll("\\r\\n;", ";");

        // Remove CR space
        result = result.replaceAll("\\r\\n ", "\r\n");

        // Remove some CRs
        result = result.replaceAll("SET\\r\\n", "SET ");
        result = result.replaceAll("INTO\\r\\n", "INTO ");
        result = result.replaceAll("FROM\\r\\n", "FROM ");
        result = result.replaceAll("FETCH\\r\\n", "FETCH ");
        result = result.replaceAll("WHERE\\r\\n", "WHERE ");

        return result;
    }

    private DB2Utils()
    {
        // Pure utility class, no instanciation allowed
    }

}
