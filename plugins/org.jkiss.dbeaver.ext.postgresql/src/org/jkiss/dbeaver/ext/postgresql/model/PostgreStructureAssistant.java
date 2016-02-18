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

import java.sql.SQLException;
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
        PostgreSchema catalog = parentObject instanceof PostgreSchema ? (PostgreSchema) parentObject : null;
        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(session, catalog, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
            findConstraintsByMask(session, catalog, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(session, catalog, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN) {
            findTableColumnsByMask(session, catalog, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_DATA_TYPE) {
            findDataTypesByMask(session, catalog, objectNameMask, maxResults, references);
        }
    }

    private void findTablesByMask(JDBCSession session, @Nullable final PostgreSchema schema, String tableNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load tables
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT x.oid,x.relname,x.relnamespace FROM pg_catalog.pg_class x " +
                "WHERE x.relkind in('r','v','m') AND x.relname LIKE ? " +
                (schema == null ? "" : " AND x.relnamespace=?") +
                " ORDER BY x.relname LIMIT " + maxResults)) {
            dbStat.setString(1, tableNameMask.toLowerCase(Locale.ENGLISH));
            if (schema != null) {
                dbStat.setInt(2, schema.getObjectId());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final int schemaId = JDBCUtils.safeGetInt(dbResult, "relnamespace");
                    final int tableId = JDBCUtils.safeGetInt(dbResult, "oid");
                    final String tableName = JDBCUtils.safeGetString(dbResult, "relname");
                    final PostgreSchema tableSchema = schema != null ? schema : dataSource.getDefaultInstance().getSchema(session.getProgressMonitor(), schemaId);
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

    private void findProceduresByMask(JDBCSession session, @Nullable final PostgreSchema schema, String procNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load procedures
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT DISTINCT x.proname,x.pronamespace FROM pg_catalog.pg_proc x " +
                "WHERE x.proname LIKE ? " +
                (schema == null ? "" : " AND x.pronamespace=?") +
                " ORDER BY x.proname LIMIT " + maxResults)) {
            dbStat.setString(1, procNameMask.toLowerCase(Locale.ENGLISH));
            if (schema != null) {
                dbStat.setInt(2, schema.getObjectId());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final int schemaId = JDBCUtils.safeGetInt(dbResult, "pronamespace");
                    final int procId = JDBCUtils.safeGetInt(dbResult, "oid");
                    final String procName = JDBCUtils.safeGetString(dbResult, "proname");
                    final PostgreSchema procSchema = schema != null ? schema : dataSource.getDefaultInstance().getSchema(session.getProgressMonitor(), schemaId);
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

    private void findConstraintsByMask(JDBCSession session, @Nullable final PostgreSchema schema, String constrNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT x.oid,x.conname,x.connamespace FROM pg_catalog.pg_constraint x " +
                "WHERE x.conname LIKE ? " +
                (schema == null ? "" : " AND x.connamespace=?") +
                " ORDER BY x.conname LIMIT " + maxResults)) {
            dbStat.setString(1, constrNameMask.toLowerCase(Locale.ENGLISH));
            if (schema != null) {
                dbStat.setInt(2, schema.getObjectId());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final int schemaId = JDBCUtils.safeGetInt(dbResult, "connamespace");
                    final int constrId = JDBCUtils.safeGetInt(dbResult, "oid");
                    final String constrName = JDBCUtils.safeGetString(dbResult, "conname");
                    final PostgreSchema constrSchema = schema != null ? schema : dataSource.getDefaultInstance().getSchema(session.getProgressMonitor(), schemaId);
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

    private void findTableColumnsByMask(JDBCSession session, @Nullable final PostgreSchema catalog, String constrNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

    }

    private void findDataTypesByMask(JDBCSession session, PostgreSchema catalog, String objectNameMask, int maxResults, List<DBSObjectReference> references) {
        DBRProgressMonitor monitor = session.getProgressMonitor();

    }

}
