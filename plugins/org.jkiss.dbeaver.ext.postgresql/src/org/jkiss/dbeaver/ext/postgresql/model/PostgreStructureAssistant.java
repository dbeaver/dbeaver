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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PostgreStructureAssistant
 */
public class PostgreStructureAssistant extends JDBCStructureAssistant
{
    private final PostgreDataSource dataSource;

    public PostgreStructureAssistant(PostgreDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    protected JDBCDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_CONSTRAINT,
            RelationalObjectType.TYPE_PROCEDURE,
            RelationalObjectType.TYPE_TABLE_COLUMN,
            RelationalObjectType.TYPE_DATA_TYPE,
            };
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_PROCEDURE
        };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_PROCEDURE,
        };
    }

    @Override
    protected void findObjectsByMask(JDBCSession session, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, boolean caseSensitive, int maxResults, List<DBSObjectReference> references) throws DBException, SQLException
    {
        PostgreSchema ownerSchema = parentObject instanceof PostgreSchema ? (PostgreSchema) parentObject : null;
        final PostgreDataSource dataSource = (PostgreDataSource) session.getDataSource();
        final PostgreDatabase database = dataSource.getDefaultInstance();
        List<PostgreSchema> nsList = new ArrayList<>();
        if (ownerSchema != null) {
            nsList.add(0, ownerSchema);
        } else {
            // Do not limit object search with search path
            // Otherwise you can't find anything else.
            // But maybe autocomplete should work only with search path???
/*
            for (String sn : dataSource.getSearchPath()) {
                final PostgreSchema schema = database.getSchema(session.getProgressMonitor(), sn);
                if (schema != null) {
                    nsList.add(schema);
                }
            }
*/
        }

        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(session, nsList, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
            findConstraintsByMask(session, nsList, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(session, nsList, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN) {
            findTableColumnsByMask(session, nsList, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_DATA_TYPE) {
            findDataTypesByMask(session, nsList, objectNameMask, maxResults, references);
        }
    }

    private void findTablesByMask(JDBCSession session, @Nullable final List<PostgreSchema> schema, String tableNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load tables
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT x.oid,x.relname,x.relnamespace FROM pg_catalog.pg_class x " +
                "WHERE x.relkind in('r','v','m') AND x.relname LIKE ? " +
                (CommonUtils.isEmpty(schema) ? "" : " AND x.relnamespace IN (?)") +
                " ORDER BY x.relname LIMIT " + maxResults)) {
            dbStat.setString(1, tableNameMask.toLowerCase(Locale.ENGLISH));
            if (!CommonUtils.isEmpty(schema)) {
                PostgreUtils.setArrayParameter(dbStat, 2, schema);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "relnamespace");
                    final long tableId = JDBCUtils.safeGetLong(dbResult, "oid");
                    final String tableName = JDBCUtils.safeGetString(dbResult, "relname");
                    final PostgreSchema tableSchema = dataSource.getDefaultInstance().getSchema(session.getProgressMonitor(), schemaId);
                    objects.add(new AbstractObjectReference(tableName, tableSchema, null, RelationalObjectType.TYPE_TABLE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            PostgreTableBase table = tableSchema.getTable(monitor, tableId);
                            if (table == null) {
                                throw new DBException("Table '" + tableName + "' not found in schema '" + tableSchema.getName() + "'");
                            }
                            return table;
                        }
                    });
                }
            }
        }
    }

    private void findProceduresByMask(JDBCSession session, @Nullable final List<PostgreSchema> schema, String procNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load procedures
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT DISTINCT x.proname,x.pronamespace FROM pg_catalog.pg_proc x " +
                "WHERE x.proname LIKE ? " +
                (CommonUtils.isEmpty(schema) ? "" : " AND x.pronamespace IN (?)") +
                " ORDER BY x.proname LIMIT " + maxResults)) {
            dbStat.setString(1, procNameMask.toLowerCase(Locale.ENGLISH));
            if (!CommonUtils.isEmpty(schema)) {
                PostgreUtils.setArrayParameter(dbStat, 2, schema);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "pronamespace");
                    final long procId = JDBCUtils.safeGetLong(dbResult, "oid");
                    final String procName = JDBCUtils.safeGetString(dbResult, "proname");
                    final PostgreSchema procSchema = dataSource.getDefaultInstance().getSchema(session.getProgressMonitor(), schemaId);
                    objects.add(new AbstractObjectReference(procName, procSchema, null, RelationalObjectType.TYPE_PROCEDURE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            PostgreProcedure procedure = procSchema.getProcedure(monitor, procName);
                            if (procedure == null) {
                                throw new DBException("Procedure '" + procName + "' not found in schema '" + procName + "'");
                            }
                            return procedure;
                        }
                    });
                }
            }
        }
    }

    private void findConstraintsByMask(JDBCSession session, @Nullable final List<PostgreSchema> schema, String constrNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT x.oid,x.conname,x.connamespace FROM pg_catalog.pg_constraint x " +
                "WHERE x.conname LIKE ? " +
                (CommonUtils.isEmpty(schema) ? "" : " AND x.connamespace IN (?)") +
                " ORDER BY x.conname LIMIT " + maxResults)) {
            dbStat.setString(1, constrNameMask.toLowerCase(Locale.ENGLISH));
            if (!CommonUtils.isEmpty(schema)) {
                PostgreUtils.setArrayParameter(dbStat, 2, schema);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "connamespace");
                    final long constrId = JDBCUtils.safeGetLong(dbResult, "oid");
                    final String constrName = JDBCUtils.safeGetString(dbResult, "conname");
                    final PostgreSchema constrSchema = dataSource.getDefaultInstance().getSchema(session.getProgressMonitor(), schemaId);
                    objects.add(new AbstractObjectReference(constrName, constrSchema, null, RelationalObjectType.TYPE_TABLE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            final PostgreTableConstraintBase constraint = PostgreUtils.getObjectById(monitor, constrSchema.constraintCache, constrSchema, constrId);
                            if (constraint == null) {
                                throw new DBException("Constraint '" + constrName + "' not found in schema '" + constrSchema.getName() + "'");
                            }
                            return constraint;
                        }
                    });
                }
            }
        }
    }

    private void findTableColumnsByMask(JDBCSession session, @Nullable final List<PostgreSchema> schema, String columnNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT x.attname,x.attrelid,x.atttypid,c.relnamespace " +
                "FROM pg_catalog.pg_attribute x, pg_catalog.pg_class c\n" +
                "WHERE c.oid=x.attrelid AND x.attname LIKE ? " +
                (CommonUtils.isEmpty(schema) ? "" : " AND c.relnamespace IN (?)") +
                " ORDER BY x.attname LIMIT " + maxResults)) {
            dbStat.setString(1, columnNameMask.toLowerCase(Locale.ENGLISH));
            if (!CommonUtils.isEmpty(schema)) {
                PostgreUtils.setArrayParameter(dbStat, 2, schema);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "relnamespace");
                    final long tableId = JDBCUtils.safeGetLong(dbResult, "attrelid");
                    final String attributeName = JDBCUtils.safeGetString(dbResult, "attname");
                    final PostgreSchema constrSchema = dataSource.getDefaultInstance().getSchema(session.getProgressMonitor(), schemaId);
                    if (constrSchema == null) {
                        log.debug("Schema '" + schemaId + "' not found");
                        continue;
                    }
                    objects.add(new AbstractObjectReference(attributeName, constrSchema, null, RelationalObjectType.TYPE_TABLE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            final PostgreTableBase table = PostgreUtils.getObjectById(monitor, constrSchema.tableCache, constrSchema, tableId);
                            if (table == null) {
                                throw new DBException("Table '" + tableId + "' not found in schema '" + constrSchema.getName() + "'");
                            }
                            return table.getAttribute(monitor, attributeName);
                        }
                    });
                }
            }
        }
    }

    private void findDataTypesByMask(JDBCSession session, List<PostgreSchema> catalog, String objectNameMask, int maxResults, List<DBSObjectReference> references) {
        DBRProgressMonitor monitor = session.getProgressMonitor();

    }

}
