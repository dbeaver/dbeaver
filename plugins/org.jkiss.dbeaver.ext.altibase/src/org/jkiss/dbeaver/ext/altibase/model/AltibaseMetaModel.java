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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.AltibaseUtils;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Altibase DataSource
 */
public class AltibaseMetaModel extends GenericMetaModel {

    private static final Log log = Log.getLog(AltibaseMetaModel.class);

    public AltibaseMetaModel() {
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new AltibaseDataSource(monitor, container, this);
    }

    @Override
    public boolean isSystemSchema(GenericSchema schema) {
        return AltibaseConstants.USER_SYSTEM_.equals(schema.getName());
    }

    @Override
    public AltibaseSchema createSchemaImpl(@NotNull GenericDataSource dataSource, 
            @Nullable GenericCatalog catalog, @NotNull String schemaName) throws DBException {
        return new AltibaseSchema(dataSource, catalog, schemaName);
    }

    @Override
    public AltibaseDataTypeCache createDataTypeCache(@NotNull GenericStructContainer container) {
        return new AltibaseDataTypeCache(container);
    }

    @Override
    public GenericTableBase createTableImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, 
            @NotNull GenericMetaObject tableObject, @NotNull JDBCResultSet dbResult) {
        String tableName = GenericUtils.safeGetStringTrimmed(tableObject, dbResult, JDBCConstants.TABLE_NAME);
        String tableType = GenericUtils.safeGetStringTrimmed(tableObject, dbResult, JDBCConstants.TABLE_TYPE);

        GenericTableBase table = super.createTableImpl(session, owner, tableObject, dbResult);

        if (table == null) {
            return null;
        }

        switch (tableType) {
            case "TABLE":
            case "SYSTEM TABLE":
                table = new AltibaseTable(owner, tableName, tableType, dbResult);
                break;
            case "VIEW":
            case "SYSTEM VIEW":
                table = new AltibaseView(owner, tableName, tableType, dbResult);
                break;
            case "MATERIALIZED VIEW":
                table = new AltibaseMaterializedView(owner, tableName, tableType, dbResult);
                break;
            case "QUEUE":
                table = new AltibaseQueue(owner, tableName, tableType, dbResult);
                break;
            case "SYNONYM":
            case "SEQUENCE":
            default:
                table = null;
        }

        return table;
    }

    @Override
    public GenericTableBase createTableOrViewImpl(
            GenericStructContainer container,
            @Nullable String tableName,
            @Nullable String tableType,
            @Nullable JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {

            if (tableType.equalsIgnoreCase(AltibaseConstants.OBJ_TYPE_MATERIALIZED_VIEW)) {
                return new AltibaseMaterializedView(
                        container,
                        tableName,
                        tableType,
                        dbResult);
            } else {
                return new AltibaseView(
                        container,
                        tableName,
                        tableType,
                        dbResult);
            }
        }

        return new AltibaseTable(
                container,
                tableName,
                tableType,
                dbResult);
    }

    /**
     * Get a specific Index DDL
     */
    public String getIndexDDL(DBRProgressMonitor monitor, AltibaseTableIndex sourceObject, 
            Map<String, Object> options) throws DBException {
        StringBuilder ddl = new StringBuilder();
        String schemaName = sourceObject.getTable().getSchema().getName();
        String ddlFromMetadata;
        
        ddlFromMetadata = getDDLFromDbmsMetadata(monitor, sourceObject, schemaName, AltibaseConstants.DBOBJ_INDEX);
        
        if (CommonUtils.isEmpty(ddlFromMetadata)) {
            ddl.append(String.format(AltibaseConstants.NO_DDL_WITHOUT_DBMS_METADATA, AltibaseConstants.DBOBJ_INDEX));
        } else {
            if (sourceObject.isSystemGenerated()) {
                ddl.append("-- System generated index. Not for user creation.")
                .append(AltibaseConstants.NEW_LINE).append("/*").append(AltibaseConstants.NEW_LINE);
            }
            
            ddl.append(ddlFromMetadata).append(";");
            
            if (sourceObject.isSystemGenerated()) {
                ddl.append(AltibaseConstants.NEW_LINE).append("*/");
            }
        }
        
        return ddl.toString();
    }
    
    @Override
    public String getTableDDL(DBRProgressMonitor monitor, GenericTableBase sourceObject, 
            Map<String, Object> options) throws DBException {
        StringBuilder ddl = new StringBuilder();
        String schemaName = sourceObject.getContainer().getName();
        String ddlFromMetadata;

        ddlFromMetadata = getDDLFromDbmsMetadata(monitor, sourceObject, schemaName, sourceObject.getTableType());
        
        if (CommonUtils.isEmpty(ddlFromMetadata)) {
            ddl.append(AltibaseConstants.NO_DBMS_METADATA).append(super.getTableDDL(monitor, sourceObject, options));
        } else {
            ddl.append(ddlFromMetadata).append(";").append(AltibaseConstants.NEW_LINE);

            // Comment
            addTableDependentDdl(ddl, "COMMENT", monitor, sourceObject, schemaName);

            // Dependent index
            for (GenericTableIndex index : sourceObject.getIndexes(monitor)) {
                AltibaseTableIndex altiIndex = (AltibaseTableIndex) index;
                if (!altiIndex.isSystemGenerated()) {
                    ddl.append(AltibaseConstants.NEW_LINE)
                        .append(altiIndex.getObjectDefinitionText(monitor, options))
                        .append(AltibaseConstants.NEW_LINE);
                }
            }
        }

        return ddl.toString();
    }
    
    /**
     * Get dependent objects of a table to table DDL, such as comment. 
     */
    private void addTableDependentDdl(StringBuilder ddl, String depObjType, 
            DBRProgressMonitor monitor, DBSObject sourceObject, String schemaName) {
        String depDdl = null;
        
        depDdl = getDepDDLFromDbmsMetadata(monitor, sourceObject, schemaName, depObjType);
        depDdl = depDdl.replaceAll("\\n\\n", AltibaseConstants.NEW_LINE); // remove empty line
        if (depDdl != null && depDdl.length() > 0) {
            ddl.append(AltibaseConstants.NEW_LINE).append(depDdl).append(AltibaseConstants.NEW_LINE);
        }
    }

    /**
     * Get a specific Synonym DDL
     */
    public String getSynonymDDL(DBRProgressMonitor monitor, AltibaseSynonym sourceObject, 
            Map<String, Object> options) throws DBException {
        return getDDLFromDbmsMetadata(monitor, sourceObject, sourceObject.getParentObject().getName(), "SYNONYM");
    }


    @Override
    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, 
            Map<String, Object> options) throws DBException {
        
        String ddl = getDDLFromDbmsMetadata(monitor, sourceObject, sourceObject.getSchema().getName(), 
                AltibaseUtils.getDmbsMetaDataObjTypeName(sourceObject.getTableType()));
        
        if (CommonUtils.isEmpty(ddl)) {
            String sql;
            if (sourceObject instanceof AltibaseMaterializedView) {
                sql = "SELECT"
                        + " parse"
                        + " FROM"
                            + " SYSTEM_.SYS_VIEW_PARSE_ VP"
                            + " ,SYSTEM_.SYS_USERS_ U"
                            + " ,SYSTEM_.SYS_MATERIALIZED_VIEWS_ MV"
                        + " WHERE"
                            + " U.USER_NAME = ?"
                            + " AND MV.MVIEW_NAME = ?"
                            + " AND VP.USER_ID = U.USER_ID"
                            + " AND VP.VIEW_ID = MV.VIEW_ID"
                        + " ORDER BY"
                            + " SEQ_NO ASC";
            } else {
                sql = "SELECT "
                        + " parse "
                    + " FROM "
                        + " SYSTEM_.SYS_VIEW_PARSE_ VP, SYSTEM_.SYS_USERS_ U, SYSTEM_.SYS_TABLES_ T"
                    + " WHERE"
                        + " U.USER_NAME = ?"
                        + " AND T.TABLE_NAME = ?"
                        + " AND T.TABLE_TYPE = 'V'"
                        + " AND VP.USER_ID = U.USER_ID"
                        + " AND VP.VIEW_ID = T.TABLE_ID"
                    + " ORDER BY SEQ_NO ASC";
            }

            ddl = getViewProcDDLFromCatalog(monitor, sourceObject, sourceObject.getSchema().getName(), sql);
        }
        
        return (ddl.length() < 1) ? "-- View definition not available" : ddl;
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        String ddl = getDDLFromDbmsMetadata(monitor, sourceObject, 
                    sourceObject.getSchema().getName(), ((AltibaseProcedureStandAlone) sourceObject).getProcedureTypeName());
        
        if (CommonUtils.isEmpty(ddl)) {
            String sql = "SELECT "
                    + " parse "
                    + " FROM "
                    + " SYSTEM_.SYS_PROC_PARSE_ PP, SYSTEM_.SYS_USERS_ U, SYSTEM_.SYS_PROCEDURES_ P"
                    + " WHERE"
                    + " U.USER_NAME = ?"
                    + " AND P.PROC_NAME = ?"
                    + " AND PP.USER_ID = U.USER_ID"
                    + " AND PP.PROC_OID = P.PROC_OID"
                    + " ORDER BY SEQ_NO ASC";
            ddl = getViewProcDDLFromCatalog(monitor, sourceObject, sourceObject.getSchema().getName(), sql);
        }

        return ddl;
    }

    @Override
    public AltibaseProcedureStandAlone createProcedureImpl(GenericStructContainer container, String procedureName, String specificName, 
            String remarks, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        return new AltibaseProcedureStandAlone(container, procedureName, specificName, remarks, procedureType, functionResultType);
    }

    /**
     * Get a specific Procedure or Package DDL
     */
    public AltibaseProcedurePackaged createProcedurePackagedImpl(GenericStructContainer container, String procedureName, 
            String specificName, String remarks, DBSProcedureType procedureType, GenericFunctionResultType functionResultType, 
            String pkgSchema) {
        return new AltibaseProcedurePackaged(container, procedureName, specificName, remarks, procedureType, functionResultType, pkgSchema);
    }

    /**
     * Get a specific Package DDL
     */
    public String getPackageDDL(DBRProgressMonitor monitor, AltibasePackage sourceObject, int packageType) throws DBException {
        boolean hasDbmsMetadata;
        String ddl = getDDLFromDbmsMetadata(monitor, sourceObject, sourceObject.getSchema().getName(), 
                    (packageType == AltibaseConstants.PACKAGE_TYPE_SPEC) ? "PACKAGE_SPEC" : "PACKAGE_BODY");
        
        hasDbmsMetadata = ddl.startsWith("--");
        
        if (hasDbmsMetadata || CommonUtils.isEmpty(ddl)) {
            String sql = "SELECT "
                    + " parse "
                    + " FROM "
                    + " SYSTEM_.SYS_PACKAGE_PARSE_ PP, SYSTEM_.SYS_USERS_ U, SYSTEM_.SYS_PACKAGES_ P"
                    + " WHERE"
                    + " U.USER_NAME = ?"
                    + " AND P.PACKAGE_NAME = ?"
                    + " AND PP.USER_ID = U.USER_ID"
                    + " AND PP.PACKAGE_OID = P.PACKAGE_OID"
                    + " AND PP.PACKAGE_TYPE = P.PACKAGE_TYPE"
                    + " AND PP.PACKAGE_TYPE = " + packageType
                    + " ORDER BY PP.PACKAGE_TYPE, SEQ_NO ASC";

            ddl = getPackageDDLFromCatalog(monitor, sourceObject, sourceObject.getSchema().getName(), sql, hasDbmsMetadata);
        }
        
        return ddl;
    }

    //////////////////////////////////////////////////////
    // Table Columns

    @Override
    public GenericTableColumn createTableColumnImpl(@NotNull DBRProgressMonitor monitor, @Nullable JDBCResultSet dbResult, 
            @NotNull GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, 
            long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, 
            String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
        return new AltibaseTableColumn(table,
                columnName,
                typeName, valueType, sourceType, ordinalPos,
                columnSize,
                charLength, scale, precision, radix, notNull,
                remarks, defaultValue, autoIncrement, autoGenerated
                );
    }

    //////////////////////////////////////////////////////
    // Sequences

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSequencesLoadStatement(@NotNull JDBCSession session, 
            @NotNull GenericStructContainer container) throws SQLException {
        final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT"
                        + " TABLE_NAME, CURRENT_SEQ, START_SEQ, INCREMENT_SEQ, CACHE_SIZE, MAX_SEQ, MIN_SEQ, IS_CYCLE"
                        + " FROM V$SEQ S, SYSTEM_.SYS_TABLES_ T, SYSTEM_.SYS_USERS_ U"
                        + " WHERE"
                        + " U.USER_NAME = ?"
                        + " AND U.USER_ID = T.USER_ID"
                        + " AND T.TABLE_OID = S.SEQ_OID"
                        + " AND T.TABLE_TYPE= 'S'"
                        + " ORDER BY TABLE_NAME ASC");
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Override
    public GenericSequence createSequenceImpl(@NotNull JDBCSession session, 
            @NotNull GenericStructContainer container, @NotNull JDBCResultSet dbResult) {
        return new AltibaseSequence(container, dbResult);
    }

    //////////////////////////////////////////////////////
    // Synonyms

    @Override
    public boolean supportsSynonyms(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSynonymsLoadStatement(@NotNull JDBCSession session, 
            @NotNull GenericStructContainer container) throws SQLException {
        boolean isPublic = container.getName().equalsIgnoreCase(AltibaseConstants.USER_PUBLIC);
        
        final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT"
                        + " NVL2(SYNONYM_OWNER_ID, SYNONYM_OWNER_ID, -1) AS SYNONYM_OWNER_ID,"
                        + " SYNONYM_NAME, OBJECT_OWNER_NAME, OBJECT_NAME"
                        + " FROM"
                        + " SYSTEM_.SYS_SYNONYMS_ S " + ((isPublic) ? "" : ", SYSTEM_.SYS_USERS_ U")
                        + " WHERE"
                        + ((isPublic) ? 
                                " SYNONYM_OWNER_ID IS NULL" 
                                : 
                                " U.USER_NAME = ? AND U.USER_ID = S.SYNONYM_OWNER_ID")
                        + " ORDER BY SYNONYM_NAME");

        if (!isPublic) {
            dbStat.setString(1, container.getName());
        }

        return dbStat;
    }

    @Override
    public GenericSynonym createSynonymImpl(@NotNull JDBCSession session, 
            @NotNull GenericStructContainer container, @NotNull JDBCResultSet dbResult) throws DBException {
        return new AltibaseSynonym(container,
                JDBCUtils.safeGetInt(dbResult, "SYNONYM_OWNER_ID"),
                JDBCUtils.safeGetString(dbResult, "SYNONYM_NAME"),
                "",
                JDBCUtils.safeGetString(dbResult, "OBJECT_OWNER_NAME"),
                JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"));
    }

    //////////////////////////////////////////////////////
    // Triggers
    
    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(@NotNull JDBCSession session, 
            @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT"
                        + "   TR.OWNER_SCHEMA"
                        + " , TR.OWNER_TABLE AS OWNER" // DBeaver dependent column alias
                        + " , TR.USER_NAME AS TRIGGER_SCHEMA"
                        + " , TR.TRIGGER_NAME"
                        + " , TR.IS_ENABLE"
                        + " , CASE2(TR.EVENT_TIME = 1, 'BEFORE', TR.EVENT_TIME = 2, 'AFTER'," 
                        + " TR.EVENT_TIME = 3, 'INSTEAD OF', 'Unknown') AS EVENT_TIME"
                        + " , CASE2(TR.EVENT_TYPE = 1, 'INSERT', TR.EVENT_TYPE = 2, 'DELETE'," 
                        + " TR.EVENT_TYPE = 4, 'UPDATE', 'Unknown') AS EVENT_TYPE "
                        + " , CASE2(TR.GRANULARITY = 1, 'FOR EACH ROW', TR.GRANULARITY = 12," 
                        + " 'TFOR EACH STATEMENT', 'Unknown') AS GRANULARITY"
                        + " , TR.UPDATE_COLUMN_CNT"
                        + " , TR.REF_ROW_CNT"
                        + " , CASE2(TDT.STMT_TYPE = 8, 'DELETE', TDT.STMT_TYPE = 19, 'INSERT'," 
                        + " TDT.STMT_TYPE = 33, 'UPDATE', "
                        + " TDT.STMT_TYPE IS NULL, NULL, 'Unknown') AS DML_STMT_TYPE"
                        + " , TDT.USER_NAME AS DMLTABLE_SCHEMA"
                        + " , TDT.TABLE_NAME AS DMLTABLE_NAME"
                        + " FROM "
                        + " (SELECT "
                        + " R.*"
                        + " ,U.USER_NAME AS OWNER_SCHEMA"
                        + " ,T.TABLE_NAME AS OWNER_TABLE"
                        + " FROM "
                        + " SYSTEM_.SYS_USERS_ U"
                        + " , SYSTEM_.SYS_TABLES_ T"
                        + " , SYSTEM_.SYS_TRIGGERS_ R "
                        + " WHERE "
                        + " U.USER_ID = T.USER_ID "
                        + " AND T.TABLE_ID = R.TABLE_ID "
                        + " AND U.USER_NAME = ?"
                        + " AND T.TABLE_NAME " + ((table == null) ? " IS NOT NULL" : "= ?") + ") TR "
                        /* left outer join: DML target table could be null */
                        + " LEFT OUTER JOIN "
                        + " (SELECT"
                        + " TRIGGER_OID"
                        + " , STMT_TYPE"
                        + " , U.USER_NAME"
                        + " , T.TABLE_NAME"
                        + " FROM "
                        + " SYSTEM_.SYS_USERS_ U"
                        + " , SYSTEM_.SYS_TABLES_ T"
                        + " , SYSTEM_.SYS_TRIGGER_DML_TABLES_"
                        + " WHERE"
                        + " DML_TABLE_ID = T.TABLE_ID"
                        + " AND T.USER_ID = U.USER_ID"
                        + " ) TDT"
                        + " ON TR.TRIGGER_OID = TDT.TRIGGER_OID");

        dbStat.setString(1, container.getName()); // user name

        if (table != null) {
            dbStat.setString(2, table.getName()); // table name
        }

        return dbStat;
    }

    @Override
    public GenericTrigger createTableTriggerImpl(@NotNull JDBCSession session, 
            @NotNull GenericStructContainer container, @NotNull GenericTableBase parent, 
            String triggerName, @NotNull JDBCResultSet dbResult) throws DBException {
        String newTriggerName = triggerName;
        
        if (CommonUtils.isEmpty(newTriggerName)) {
            newTriggerName = JDBCUtils.safeGetStringTrimmed(dbResult, "TRIGGER_NAME");
        }
        if (newTriggerName == null) {
            return null;
        }

        return new AltibaseTableTrigger(
                parent,
                newTriggerName,
                null, //description
                dbResult);
    }

    @Override
    public boolean supportsDatabaseTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, 
            @NotNull GenericTrigger trigger) throws DBException {
        String ddl = null;
        String schemaName = null;

        if (trigger.getParentObject() instanceof GenericTable) {
            schemaName = trigger.getParentObject().getParentObject().getName();
        } else {
            schemaName = trigger.getParentObject().getName();
        }

        ddl = getDDLFromDbmsMetadata(monitor, trigger, schemaName, "TRIGGER");
        
        if (CommonUtils.isEmpty(ddl)) {
            String sql = "SELECT "
                    + " substring"
                    + " FROM"
                    + " system_.sys_triggers_ t"
                    + " , system_.sys_trigger_strings_ sts"
                    + " WHERE"
                    + " t.user_name = ?"
                    + " AND t.trigger_name = ?"
                    + " AND sts.trigger_oid = t.trigger_oid"
                    + " ORDER BY seqno";

            ddl = getTriggerDDLFromCatalog(monitor, trigger, schemaName, sql);
        }
        
        return ddl;
    }

    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    //////////////////////////////////////////////////////
    // Constraints

    @Override
    public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session, 
            @NotNull GenericStructContainer owner, @Nullable GenericTableBase forParent) throws SQLException {

        boolean hasParent = (forParent != null);
        StringBuilder qry = new StringBuilder("SELECT"
                + " (SELECT db_name FROM v$database) AS TABLE_CAT,"
                + " u.user_name AS TABLE_SCHEME,"
                + " t.table_name AS TABLE_NAME,"
                + " col.column_name AS COLUMN_NAME, "
                + " (ccol.constraint_col_order + 1) AS KEY_SEQ,"
                + " c.constraint_name AS PK_NAME, "
                + " c.constraint_type, "
                + " c.check_condition, "
                + " c.validated"
                + " FROM"
                + " system_.sys_users_ u, system_.sys_tables_ t, system_.sys_columns_ col,"
                + " system_.sys_constraints_ c, system_.sys_constraint_columns_ ccol"
                + " WHERE"
                + " u.user_name = ?"

                + " AND u.user_id = c.user_id"
                + " AND u.user_id = t.user_id"
                + " AND t.table_id = c.table_id"
                + " AND c.constraint_type != 0"
                + " AND c.constraint_id = ccol.constraint_id"
                + " AND ccol.column_id = col.column_id");
        
        if (hasParent) {
            qry.append(" AND t.table_name = ?");
        }

        final JDBCPreparedStatement dbStat = session.prepareStatement(qry.toString());

        dbStat.setString(1, owner.getName());
        
        if (hasParent) {
            dbStat.setString(2, forParent.getName());
        }
        
        return dbStat;
    }

    @Override
    public GenericUniqueKey createConstraintImpl(GenericTableBase table, String constraintName, 
            DBSEntityConstraintType constraintType, JDBCResultSet dbResult, boolean persisted) {
        
        if (dbResult != null) {            
            String condition = constraintType.equals(DBSEntityConstraintType.CHECK) ? 
                    JDBCUtils.safeGetString(dbResult, "CHECK_CONDITION") : "";
            boolean validated = JDBCUtils.safeGetString(dbResult, "VALIDATED").equals("T");
            return new AltibaseConstraint(table, constraintName, null, constraintType, persisted, condition, validated);
        } else {
            return super.createConstraintImpl(table, constraintName, constraintType, dbResult, persisted);
        }
    }

    @Override
    public DBSEntityConstraintType getUniqueConstraintType(JDBCResultSet dbResult) throws DBException, SQLException {

        int constraintType = JDBCUtils.safeGetInt(dbResult, "CONSTRAINT_TYPE");

        switch (constraintType) {
            case 1:
                return DBSEntityConstraintType.NOT_NULL;
            case 2:
                return DBSEntityConstraintType.UNIQUE_KEY;
            case 3:
                return DBSEntityConstraintType.PRIMARY_KEY;
            case 7:
                return DBSEntityConstraintType.CHECK;
            case 5:
                return AltibaseConstraint.TIMESTAMP;
            case 6:
                return AltibaseConstraint.LOCAL_UNIQUE_KEY;
            /* Foreign key must be handled separately: c.constraint_type != 0 
            case 0:
                return DBSEntityConstraintType.FOREIGN_KEY;
            */

            default:
                String exMsg = String.format("Unknown constraint type [NAME] %s [TYPE] %d", 
                        JDBCUtils.safeGetString(dbResult, "PK_NAME"), constraintType);
                log.error(exMsg);
                throw new DBException(exMsg);
        }
    }

    //////////////////////////////////////////////////////
    // Packages

    /**
     * Get Packages name and package object
     */
    private void loadPackages(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container, 
            JDBCSession session, Map<String, AltibasePackage> packageMap) throws SQLException, DBException {

        String schemaName = container.getName();

        // Load packages
        try (JDBCStatement dbStat = preparePackageLoadStatement(session, container)) {
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            dbStat.executeStatement();
            JDBCResultSet dbResult = dbStat.getResultSet();
            if (dbResult != null) {
                try {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }

                        String packageName = JDBCUtils.safeGetString(dbResult, "PACKAGE_NAME");
                        String key = schemaName + "." + packageName;
                        AltibasePackage altibasePackage = packageMap.get(key);

                        // new package
                        if (altibasePackage == null) {

                            altibasePackage = createPackageImpl(container, packageName, dbResult);

                            if (altibasePackage != null) {
                                container.addPackage(altibasePackage);
                                packageMap.put(key, altibasePackage);
                            } else {
                                log.warn("Fail to create Package: " + key);
                            }
                        } else {
                            // if body found,
                            if (JDBCUtils.safeGetInt(dbResult, "PACKAGE_TYPE") == 7) {
                                altibasePackage.setBody(true);
                            } else {
                                // can't be here
                                throw new DBException("Duplicated package name found: " + key);
                            }
                        }
                    }
                } finally {
                    dbResult.close();
                }
            }
        }
    }

    /**
     * Get Package dependent procedure/function
     */
    private void loadPackageDepedentProcedures(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container, 
            JDBCSession session, Map<String, AltibasePackage> packageMap) throws SQLException, DBException {

        String pkgSchema = container.getName();

        // Load packages
        try (JDBCStatement dbStat = prepareProcedurePackagedLoadStatement(session, container)) {
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            dbStat.executeStatement();
            JDBCResultSet dbResult = dbStat.getResultSet();
            if (dbResult != null) {
                try {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }

                        String packageName = JDBCUtils.safeGetString(dbResult, "PACKAGE_NAME");
                        String procName = JDBCUtils.safeGetString(dbResult, "SUB_PROC_NAME");
                        int subType = JDBCUtils.safeGetInt(dbResult, "SUB_TYPE");

                        if (CommonUtils.isEmpty(procName)) {
                            continue;
                        }

                        String key = pkgSchema + "." + packageName;
                        AltibasePackage pkg = packageMap.get(key);

                        if (pkg != null) {
                            final AltibaseProcedurePackaged procedure = createProcedurePackagedImpl(
                                    pkg,
                                    procName,
                                    procName,
                                    "",
                                    (subType == 0) ? DBSProcedureType.PROCEDURE : DBSProcedureType.FUNCTION,
                                            null,
                                            pkgSchema);

                            if (procedure != null) {
                                pkg.addProcedure(procedure);
                            }
                        }

                    }
                } finally {
                    dbResult.close();
                }
            }
        }
    }

    /**
     * Get Procedure/Function/Typeset
     */
    private void loadPSMs(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container, 
            JDBCSession session) throws SQLException {

        GenericDataSource dataSource = container.getDataSource();
        GenericMetaObject procObject = dataSource.getMetaObject(GenericConstants.OBJECT_PROCEDURE);

        try (JDBCResultSet dbResult = session.getMetaData().getProcedures(
                container.getCatalog() == null ? null : container.getCatalog().getName(),
                        container.getSchema() == null || DBUtils.isVirtualObject(container.getSchema()) ? 
                                null : JDBCUtils.escapeWildCards(session, container.getSchema().getName()),
                                dataSource.getAllObjectsPattern())) {

            while (dbResult.next()) {
                if (monitor.isCanceled()) {
                    break;
                }
                String procedureName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_NAME);
                String specificName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.SPECIFIC_NAME);
                int procTypeNum = GenericUtils.safeGetInt(procObject, dbResult, JDBCConstants.PROCEDURE_TYPE);
                String remarks = GenericUtils.safeGetString(procObject, dbResult, JDBCConstants.REMARKS);
                DBSProcedureType procedureType;

                switch (procTypeNum) {
                    case DatabaseMetaData.procedureNoResult:
                        procedureType = DBSProcedureType.PROCEDURE;
                        break;
                    case DatabaseMetaData.procedureReturnsResult:
                        procedureType = DBSProcedureType.FUNCTION;
                        break;
                        // Typeset
                    case DatabaseMetaData.procedureResultUnknown:
                        procedureType = DBSProcedureType.UNKNOWN;
                        break;

                    default:
                        continue;
                }
                if (CommonUtils.isEmpty(specificName)) {
                    specificName = procedureName;
                }

                // Typeset
                if (procedureType == DBSProcedureType.UNKNOWN) {
                    final AltibaseTypeset typeset = createTypesetImpl(container, procedureName);
                    container.addProcedure(typeset);
                } else {
                    // Procedure or Function
                    final AltibaseProcedureStandAlone procedure = createProcedureImpl(
                            container,
                            procedureName,
                            specificName,
                            remarks,
                            procedureType,
                            null);

                    if (procedure != null) {
                        container.addProcedure(procedure);
                    }
                }

            }
        } 
    }

    //////////////////////////////////////////////////////
    // Procedure load

    /**
     * Altibase JDBC getProcedures method returns procedures and functions together
     */
    public void loadProcedures(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container)
            throws DBException {
        // Key: schemaName.objName, Value: AltibasePackage
        Map<String, AltibasePackage> packageMap = new HashMap<String, AltibasePackage>();

        GenericDataSource dataSource = container.getDataSource();

        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load procedures")) {

            loadPackages(monitor, container, session, packageMap);

            loadPackageDepedentProcedures(monitor, container, session, packageMap); 

            loadPSMs(monitor, container, session);

        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    /**
     * Create Altibase Typeset object
     */
    public AltibaseTypeset createTypesetImpl(GenericStructContainer container, String procedureName) {
        return new AltibaseTypeset(container, procedureName);
    }

    //////////////////////////////////////////////////////
    // Packages
    
    /**
     * Statement to load packages
     */
    public JDBCStatement preparePackageLoadStatement(JDBCSession session, 
            GenericStructContainer container) throws SQLException {
        final JDBCPreparedStatement dbStat = session.prepareStatement(
                /* Name, type ordering is required to package specification is prior to body. */
                "SELECT"
                        + " PACKAGE_NAME, PACKAGE_TYPE, AUTHID, STATUS"
                        + " FROM SYSTEM_.SYS_PACKAGES_ P, SYSTEM_.SYS_USERS_ U"
                        + " WHERE"
                        + " U.USER_NAME = ?"
                        + " AND U.USER_ID = P.USER_ID"
                        + " ORDER BY PACKAGE_NAME, PACKAGE_TYPE ASC"); 
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    /**
     * Create Package implementation
     */
    public AltibasePackage createPackageImpl(GenericStructContainer container, 
            String packageName, JDBCResultSet resultSet) {
        return new AltibasePackage(container, packageName, resultSet);
    }

    /**
     * Statement to load package dependent procedure/function
     */
    public JDBCStatement prepareProcedurePackagedLoadStatement(JDBCSession session, 
            GenericStructContainer container) throws SQLException {
        final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT"
                        + " PP.PACKAGE_NAME"
                        + ", PP.OBJECT_NAME AS SUB_PROC_NAME"
                        + ", PP.SUB_TYPE" /* SUB_TYPE 0: procedure, 1: function */
                        + " FROM SYSTEM_.SYS_PACKAGES_ P, SYSTEM_.SYS_USERS_ U, SYSTEM_.SYS_PACKAGE_PARAS_ PP"
                        + " WHERE"
                        + " U.USER_NAME = ?"
                        + " AND U.USER_ID = P.USER_ID"
                        + " AND P.PACKAGE_OID = PP.PACKAGE_OID"
                        + " AND ( PARA_ORDER = 1 OR PARA_ORDER = 0 )"); /* 0 is if no parameter, 1 is the first parameter if any */
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    /**
     * Statement to load package dependent procedure/function columns
     */
    public JDBCPreparedStatement prepareProcedurePackagedColumnLoadStatement(JDBCSession session, 
            String pkgSchema, String pkgName, String procName) throws SQLException {
        final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT"
                        + " PP.*, D.TYPE_NAME"
                        + " FROM SYSTEM_.SYS_USERS_ U, SYSTEM_.SYS_PACKAGE_PARAS_ PP, V$DATATYPE D"
                        + " WHERE"
                        + " U.USER_NAME = ?"
                        + " AND PP.PACKAGE_NAME = ?"
                        + " AND PP.OBJECT_NAME = ?"
                        + " AND U.USER_ID = PP.USER_ID"
                        + " AND PP.DATA_TYPE = D.DATA_TYPE"
                        + " ORDER BY PARA_ORDER ASC");
        dbStat.setString(1, pkgSchema);
        dbStat.setString(2, pkgName);
        dbStat.setString(3, procName);
        return dbStat;
    }

    /**
     * Get DDL source for View/Procedure/Function/Typeset
     */
    private String getViewProcDDLFromCatalog(DBRProgressMonitor monitor, DBSObject sourceObject, String schemaName, String sql) {
        return geDDLFromCatalog(monitor, sourceObject, schemaName, sql, "PARSE", false);
    }
    
    /**
     * Get DDL source for Package
     */
    private String getPackageDDLFromCatalog(DBRProgressMonitor monitor, DBSObject sourceObject, String schemaName, String sql, 
            boolean hasDbmsMetdata) {
        return geDDLFromCatalog(monitor, sourceObject, schemaName, sql, "PARSE", hasDbmsMetdata);
    }

    /**
     * Get DDL source for Trigger
     */
    private String getTriggerDDLFromCatalog(DBRProgressMonitor monitor, DBSObject sourceObject, String schemaName, String sql) {
        return geDDLFromCatalog(monitor, sourceObject, schemaName, sql, "SUBSTRING", false);
    }

    /**
     * Get DDL source from DBMS catalog (meta tables): View/Procedure/Function/Typeset/Trigger
     */
    private String geDDLFromCatalog(DBRProgressMonitor monitor, DBSObject sourceObject, String schemaName, String sql, 
            String colname, boolean hasDbmsMetdata) {
        StringBuilder ddl = new StringBuilder(hasDbmsMetdata ? "" : AltibaseConstants.NO_DBMS_METADATA);
        String content = null;
        JDBCPreparedStatement jpstmt = null;
        JDBCResultSet jrs = null;
        GenericMetaObject metaObject = getMetaObject(GenericConstants.OBJECT_TABLE);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Get DDL from DB")) {
            jpstmt = session.prepareStatement(sql);
            jpstmt.setString(1, schemaName);
            jpstmt.setString(2, sourceObject.getName());

            jrs = jpstmt.executeQuery();
            while (jrs.next()) {
                if (monitor.isCanceled()) {
                    break;
                }

                content = GenericUtils.safeGetString(metaObject, jrs, colname);
                if (content != null) {
                    ddl.append(content);
                }
            }
        } catch (Exception e) {
            log.warn("Can't read DDL", e);
        } finally {
            jrs.close();
            jpstmt.close();
        }

        return ddl.toString();
    }

    /**
     * Get DDL from DBMS_METADATA package
     */
    private String getDDLFromDbmsMetadata(DBRProgressMonitor monitor, DBSObject sourceObject, String schemaName, String objectType) {
        String ddl = "";
        CallableStatement cstmt = null;
        
        /* Reference:
         * Need to use native CallableStatement
            jcstmt = session.prepareCall("exec ? := dbms_metadata.get_ddl(?, ?, ?)");
            java.lang.NullPointerException
            at Altibase.jdbc.driver.AltibaseParameterMetaData.getParameterMode(AltibaseParameterMetaData.java:31)
            at org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCCallableStatementImpl.getOutputParametersFromJDBC
            (JDBCCallableStatementImpl.java:316)
            at org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCCallableStatementImpl.<init>(JDBCCallableStatementImpl.java:115)
            at org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCFactoryDefault.createCallableStatement(JDBCFactoryDefault.java:48)
         */
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Get DDL from DBMS_METADATA")) {
            if (hasDbmsMetadataPacakge(session)) {
                Connection conn = session.getOriginal();
                cstmt = conn.prepareCall("exec ? := dbms_metadata.get_ddl(?, ?, ?)");
                cstmt.registerOutParameter(1, Types.VARCHAR);

                cstmt.setString(2, objectType);
                cstmt.setString(3, sourceObject.getName());
                cstmt.setString(4, schemaName);

                cstmt.execute();

                ddl = cstmt.getString(1);
            }
        } catch (SQLException se) {
            // Invalid data length.
            if (se.getSQLState().equals(AltibaseConstants.SQL_STATE_TOO_LONG)) {
                ddl = "-- DDL is too long to be fetched.";
            }
        } catch (Exception e) {
            log.warn("Can't read DDL from DBMS_METADATA", e);
        } finally {
            try {
                if (cstmt != null) {
                    cstmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return ddl;
    }

    /**
     * Get DDL from DBMS_METADATA package in case of dependent DDL.
     */
    private String getDepDDLFromDbmsMetadata(DBRProgressMonitor monitor, DBSObject sourceObject, 
            String schemaName, String depObjectType) {
        String ddl = "";
        String sqlTerm = "SQLTERMINATOR";
        String getDepDdlQry = "SELECT dbms_metadata.get_dependent_ddl('%s', '%s', '%s') FROM DUAL";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Get Dependent DDL from DBMS_METADATA")) {
            if (hasDbmsMetadataPacakge(session)) {
                conn = session.getOriginal();
                
                // SQLTERMINATOR: T
                setTransformParam(conn, sqlTerm, "T");

                // get dependent ddl 
                stmt = conn.createStatement();
                rs = stmt.executeQuery(String.format(getDepDdlQry, depObjectType, sourceObject.getName(), schemaName));
                
                if (rs.next()) {
                    ddl = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() != AltibaseConstants.EC_DBMS_METADATA_NOT_FOUND) {
                log.warn("Failed to get dbms_metadata.get_dependent_ddl [TYPE] " + depObjectType 
                        + " [BASE OBJECT] " + schemaName + "." + sourceObject.getName(), e);
            }
        } catch (Exception e) {
            log.warn("Failed to get dbms_metadata.get_dependent_ddl [TYPE] " + depObjectType 
                    + " [BASE OBJECT] " + schemaName + "." + sourceObject.getName(), e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                
                if (stmt != null) {
                    stmt.close();
                }
                
                // SQLTERMINATOR: F
                if (conn != null) {
                    setTransformParam(conn, sqlTerm, "F");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } 
        }

        return ddl;
    }
    
    /**
     * Set dbms_metadata.set_transform_param to change its output format 
     * such as put at the end of returned query with SQL Terminator
     */
    private boolean setTransformParam(Connection conn, String key, String value) throws SQLException {
        boolean result = false;
        CallableStatement cstmt = null;

        try {
            cstmt = conn.prepareCall("exec dbms_metadata.set_transform_param(?, ?)");
            cstmt.setString(1, key);
            cstmt.setString(2, value);
            result = cstmt.execute();
        } catch (SQLException e) {
            log.warn("Failed to execute dbms_metadata.set_transform_param [KEY]" + key + ", [VALUE]" + value, e); 
            
            if (cstmt != null) {
                cstmt.close();
            }
        }
        
        return result;
    }
    
    /**
     * Check whether the connected DBMS has DBMS_METADATA package or not.
     */
    private boolean hasDbmsMetadataPacakge(JDBCSession session) {
        boolean hasDbmsMetadataPacakge = false;

        String sql = "SELECT "
                + " count(*)"
                + " FROM "
                + " SYSTEM_.SYS_PACKAGES_ P" 
                + " WHERE"
                + " PACKAGE_NAME = 'DBMS_METADATA' "
                + " AND STATUS = 0"; // valid

        try (JDBCPreparedStatement jpstmt = session.prepareStatement(sql)) {
            JDBCResultSet jrs =  jpstmt.executeQuery();
            if (jrs.next()) {
                hasDbmsMetadataPacakge = (jrs.getInt(1) == 2);
            }

            jrs.close();
        } catch (Exception e) {
            log.warn("Can't check DBMS_METADATA", e);
        }

        return hasDbmsMetadataPacakge;
    }
    
    @Override
    public AltibaseTableIndex createIndexImpl(
            GenericTableBase table,
            boolean nonUnique,
            String qualifier,
            long cardinality,
            String indexName,
            DBSIndexType indexType,
            boolean persisted) {
        return new AltibaseTableIndex(
                table,
                nonUnique,
                qualifier,
                cardinality,
                indexName,
                indexType,
                persisted);
    }
}
