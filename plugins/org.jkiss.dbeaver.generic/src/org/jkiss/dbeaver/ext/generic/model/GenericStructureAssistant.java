/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
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
import java.util.List;

/**
 * GenericDataSource
 */
public class GenericStructureAssistant extends JDBCStructureAssistant
{
    private final GenericDataSource dataSource;

    public GenericStructureAssistant(GenericDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    protected GenericDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_PROCEDURE
            };
    }

    @Override
    protected void findObjectsByMask(JDBCExecutionContext context, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, boolean caseSensitive, int maxResults, List<DBSObjectReference> references) throws DBException, SQLException
    {
        GenericSchema schema = parentObject instanceof GenericSchema ? (GenericSchema)parentObject : null;
        GenericCatalog catalog = parentObject instanceof GenericCatalog ? (GenericCatalog)parentObject :
            schema == null ? null : schema.getCatalog();
        final GenericDataSource dataSource = getDataSource();
        DBPIdentifierCase convertCase = caseSensitive ? dataSource.getInfo().storesQuotedCase() : dataSource.getInfo().storesUnquotedCase();
        objectNameMask = convertCase.transform(objectNameMask);

        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(context, catalog, schema, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(context, catalog, schema, objectNameMask, maxResults, references);
        }
    }

    private void findTablesByMask(JDBCExecutionContext context, GenericCatalog catalog, GenericSchema schema, String tableNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();
        JDBCResultSet dbResult = context.getMetaData().getTables(
            catalog == null ? null : catalog.getName(),
            schema == null ? null : schema.getName(),
            tableNameMask,
            null);
        try {
            while (dbResult.next()) {
                if (monitor.isCanceled()) {
                    break;
                }
                String catalogName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_CAT);
                String schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_SCHEM);
                String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_NAME);
                if (CommonUtils.isEmpty(tableName)) {
                    continue;
                }
                objects.add(new TableReference(catalog, schema, catalogName, schemaName, tableName, JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS)));
                if (objects.size() >= maxResults) {
                    break;
                }
            }
        }
        finally {
            dbResult.close();
        }
    }

    private void findProceduresByMask(JDBCExecutionContext context, GenericCatalog catalog, GenericSchema schema, String procNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();
        JDBCResultSet dbResult = context.getMetaData().getProcedures(
            catalog == null ? null : catalog.getName(),
            schema == null ? null : schema.getName(),
            procNameMask);
        try {
            while (dbResult.next()) {
                if (monitor.isCanceled()) {
                    break;
                }
                String catalogName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PROCEDURE_CAT);
                String schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PROCEDURE_SCHEM);
                String procName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PROCEDURE_NAME);
                String uniqueName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.SPECIFIC_NAME);
                if (CommonUtils.isEmpty(procName)) {
                    continue;
                }
                if (CommonUtils.isEmpty(uniqueName)) {
                    uniqueName = procName;
                }
                objects.add(new ProcedureReference(catalog, schema, catalogName, schemaName, procName, uniqueName));
                if (objects.size() >= maxResults) {
                    break;
                }
            }
        }
        finally {
            dbResult.close();
        }
    }

    private abstract class ObjectReference extends AbstractObjectReference {
        protected final GenericCatalog parentCatalog;
        protected final GenericSchema parentSchema;
        protected final String catalogName;
        protected final String schemaName;

        protected ObjectReference(GenericCatalog parentCatalog, GenericSchema parentSchema, String catalogName, String schemaName, String name, String description, DBSObjectType type)
        {
            super(name, parentSchema != null ? parentSchema : parentCatalog, description, type);
            this.parentCatalog = parentCatalog;
            this.parentSchema = parentSchema;
            this.catalogName = catalogName;
            this.schemaName = schemaName;
        }
    }

    private class TableReference extends ObjectReference {

        private TableReference(GenericCatalog parentCatalog, GenericSchema parentSchema, String catalogName, String schemaName, String tableName, String description)
        {
            super(parentCatalog, parentSchema, catalogName, schemaName, tableName, description, RelationalObjectType.TYPE_TABLE);
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
        {
            GenericCatalog tableCatalog = parentCatalog != null ? parentCatalog : CommonUtils.isEmpty(catalogName) ? null : dataSource.getCatalog(catalogName);
            if (tableCatalog == null && CommonUtils.isEmpty(catalogName) && !CommonUtils.isEmpty(dataSource.getCatalogs()) && dataSource.getCatalogs().size() == 1) {
                // there is only one catalog - let's use it (PostgreSQL)
                tableCatalog = dataSource.getCatalogs().iterator().next();
            }
            GenericSchema tableSchema = parentSchema != null ?
                parentSchema :
                CommonUtils.isEmpty(schemaName) ? null :
                    tableCatalog == null ? dataSource.getSchema(schemaName) : tableCatalog.getSchema(monitor, schemaName);
            GenericTable table;
            if (tableSchema != null) {
                table = tableSchema.getTable(monitor, getName());
            } else if (tableCatalog != null) {
                table = tableCatalog.getTable(monitor, getName());
            } else {
                table = dataSource.getTable(monitor, getName());
            }
            if (table == null) {
                throw new DBException("Can't find table '" + getName() + "' in '" + catalogName + "/" + schemaName + "'");
            }
            return table;
        }
    }

    private class ProcedureReference extends ObjectReference {

        private String uniqueName;
        private ProcedureReference(GenericCatalog parentCatalog, GenericSchema parentSchema, String catalogName, String schemaName, String procedureName, String uniqueName)
        {
            super(parentCatalog, parentSchema, catalogName, schemaName, procedureName, null, RelationalObjectType.TYPE_PROCEDURE);
            this.uniqueName = uniqueName;
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
        {
            GenericCatalog procCatalog = parentCatalog != null ? parentCatalog : CommonUtils.isEmpty(catalogName) ? null : dataSource.getCatalog(catalogName);
            GenericSchema procSchema = parentSchema != null ?
                parentSchema :
                CommonUtils.isEmpty(schemaName) ? null :
                    procCatalog == null ? dataSource.getSchema(schemaName) : procCatalog.getSchema(monitor, schemaName);
            GenericProcedure procedure = null;
            if (procSchema != null) {
                // Try to use catalog name as package name (Oracle)
                if (!CommonUtils.isEmpty(catalogName)) {
                    GenericPackage procPackage = procSchema.getPackage(monitor, catalogName);
                    if (procPackage != null) {
                        procedure = procPackage.getProcedure(monitor, uniqueName);
                    }
                }
                if (procedure == null) {
                    procedure = procSchema.getProcedure(monitor, uniqueName);
                }
            } else if (procCatalog != null) {
                procedure = procCatalog.getProcedure(monitor, uniqueName);
            } else {
                procedure = dataSource.getProcedure(monitor, uniqueName);
            }
            if (procedure == null) {
                throw new DBException("Can't find procedure '" + getName() + "' (" + uniqueName + ")" + "' in '" + catalogName + "/" + schemaName + "'");
            }
            return procedure;
        }
    }
}
