/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HANAMetaModel
 */
public class HANAMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(HANAMetaModel.class);
    private static Pattern ERROR_POSITION_PATTERN = Pattern.compile(" \\(at pos ([0-9]+)\\)");

    public HANAMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new HANADataSource(monitor, container, this);
    }

    @Override
    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read HANA view source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT DEFINITION\n" +
                    "FROM SYS.VIEWS\n" +
                    "WHERE SCHEMA_NAME=? and VIEW_NAME=?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return
                            "CREATE VIEW " + sourceObject.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS\n" +
                            dbResult.getString(1);
                    }
                    return "-- HANA view definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read HANA procedure source")) {
            String procedureType = sourceObject.getProcedureType().name();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT SCHEMA_NAME,"+ procedureType + "_NAME,DEFINITION FROM SYS."+ procedureType + "S\n" +
                    "WHERE SCHEMA_NAME = ? AND " + procedureType + "_NAME = ?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return dbResult.getString(3);
                    }
                    return "-- HANA procedure source not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public String getTableDDL(DBRProgressMonitor monitor, GenericTableBase sourceObject, Map<String, Object> options) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read HANA table DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareCall(
                "CALL get_object_definition(?,?)"))
            {
                dbStat.setString(1, DBUtils.getQuotedIdentifier(sourceObject.getContainer()));
                dbStat.setString(2, DBUtils.getQuotedIdentifier(sourceObject));
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder ddl = new StringBuilder();
                    while (dbResult.nextRow()) {
                        ddl.append(dbResult.getString("OBJECT_CREATION_STATEMENT"));
                    }
                    if (ddl.length() > 0) {
                        // Format DDL
                        return SQLFormatUtils.formatSQL(sourceObject.getDataSource(), ddl.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error reading DDL from HANA server", e);
        }

        return super.getTableDDL(monitor, sourceObject, options);
    }

    @Override
    public boolean supportsTableDDLSplit(GenericTableBase sourceObject) {
        return false;
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }
    
    @Override
    public List<? extends GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws DBException {
        if (table == null) {
            return Collections.emptyList();
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT TRIGGER_NAME FROM SYS.TRIGGERS WHERE SUBJECT_TABLE_NAME=?")) {
                dbStat.setString(1, table.getName());
                List<GenericTrigger> result = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        result.add(new GenericTrigger(container, table, name, null));
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Override
    public String getTriggerDDL(DBRProgressMonitor monitor, GenericTrigger sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read HANA trigger source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT SCHEMA_NAME,TRIGGER_NAME,DEFINITION FROM SYS.TRIGGERS\n" +
                    "WHERE SCHEMA_NAME = ? AND TRIGGER_NAME = ?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return dbResult.getString(3);
                    }
                    return "-- HANA trigger source not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public boolean supportsSequences(GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(DBRProgressMonitor monitor, GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read synonyms")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT SEQUENCE_NAME, MIN_VALUE, MAX_VALUE, INCREMENT_BY FROM SYS.SEQUENCES WHERE SCHEMA_NAME = ? ORDER BY SEQUENCE_NAME")) {
                dbStat.setString(1, container.getName());
                List<GenericSequence> result = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = dbResult.getString(1);
                        Number minValue = dbResult.getBigDecimal(2);
                        Number maxValue = dbResult.getBigDecimal(3);
                        Number incrementBy = dbResult.getBigDecimal(4);
                        Number lastValue = null;
                        GenericSequence sequence = new GenericSequence(container, name, "", lastValue, minValue, maxValue, incrementBy);
                        result.add(sequence);
                    }
                }
                return result;

            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    
    @Override
    public boolean supportsSynonyms(GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<? extends GenericSynonym> loadSynonyms(DBRProgressMonitor monitor, GenericStructContainer container) throws DBException {
        // TODO: create a fake schema to show PUBLIC synonyms? 
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read synonyms")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT SYNONYM_NAME, OBJECT_SCHEMA, OBJECT_NAME FROM SYS.SYNONYMS WHERE SCHEMA_NAME = ? ORDER BY SYNONYM_NAME")) {
                dbStat.setString(1, container.getName());
                List<GenericSynonym> result = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = dbResult.getString(1);
                        String targetSchema = dbResult.getString(2);
                        String targetObject = dbResult.getString(3);
                        HANASynonym synonym = new HANASynonym(container, name, targetSchema, targetObject);
                        result.add(synonym);
                    }
                }
                return result;

            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

	@Override
	public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session,
			@NotNull GenericStructContainer owner, @Nullable GenericTableBase forParent) throws SQLException {
		JDBCPreparedStatement dbStat;
		if(forParent!=null) { 
			dbStat = session.prepareStatement("SELECT"
					+ " TABLE_NAME, COLUMN_NAME, POSITION AS KEY_SEQ, CONSTRAINT_NAME AS PK_NAME, IS_PRIMARY_KEY" 
					+ " FROM SYS.CONSTRAINTS"
					+ " WHERE SCHEMA_NAME=? AND TABLE_NAME=?"
					+ " ORDER BY PK_NAME");
			dbStat.setString(1, forParent.getSchema().getName());
			dbStat.setString(2, forParent.getName());
		} else {
			dbStat = session.prepareStatement("SELECT"
					+ " TABLE_NAME, COLUMN_NAME, POSITION AS KEY_SEQ, CONSTRAINT_NAME AS PK_NAME, IS_PRIMARY_KEY" 
					+ " FROM SYS.CONSTRAINTS"
					+ " ORDER BY PK_NAME");
		}
		return dbStat;
	}

	@Override
	public DBSEntityConstraintType getUniqueConstraintType(JDBCResultSet dbResult) throws DBException, SQLException {
		String isPrimaryKey = JDBCUtils.safeGetString(dbResult, "IS_PRIMARY_KEY");
		return "TRUE".equals(isPrimaryKey) ? DBSEntityConstraintType.PRIMARY_KEY : DBSEntityConstraintType.UNIQUE_KEY;
	}
	
    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "GENERATED ALWAYS AS IDENTITY";
    }

    @Override
    public boolean isSystemSchema(GenericSchema schema) {
        String schemaName = schema.getName();
        return schemaName.startsWith("_SYS_") ||
            schemaName.startsWith("SAP_") ||
            schemaName.startsWith("HANA_");
    }

    @Override
    public boolean isSystemTable(GenericTableBase table) {
        // empty schemas are still shown, so hiding everything in system schemas looks strange
        //if (table.getSchema().getName().startsWith("_SYS_"))
        //    return true;
        return table.getName().startsWith("_SYS_");
    }
    
    @Override
    public DBPErrorAssistant.ErrorPosition getErrorPosition(@NotNull Throwable error) {
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.line = -1;
                pos.position = Integer.parseInt(matcher.group(1)) - 1;
                return pos;
            }
        }
        return null;
    }
    
    @Override
    public GenericTableColumn createTableColumnImpl(DBRProgressMonitor monitor, GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {

        if(table.getSchema().getName().equals("SYS") && table.isView()) {
            ((HANADataSource)table.getDataSource()).initializeSysViewColumnUnits(monitor);
            return new HANASysViewColumn(table,
                    columnName,
                    typeName, valueType, sourceType, ordinalPos,
                    columnSize,
                    charLength, scale, precision, radix, notNull,
                    remarks, defaultValue, autoIncrement, autoGenerated
                );
        } else {
            return new HANATableColumn(table,
                columnName,
                typeName, valueType, sourceType, ordinalPos,
                columnSize,
                charLength, scale, precision, radix, notNull,
                remarks, defaultValue, autoIncrement, autoGenerated
            );
        }
    }
    
}
