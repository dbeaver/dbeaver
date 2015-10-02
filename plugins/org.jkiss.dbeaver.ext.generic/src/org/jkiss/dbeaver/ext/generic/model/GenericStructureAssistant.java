/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;
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

    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return getSupportedObjectTypes();
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return getSupportedObjectTypes();
    }

    @Override
    protected void findObjectsByMask(JDBCSession session, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, boolean caseSensitive, int maxResults, List<DBSObjectReference> references) throws DBException, SQLException
    {
        GenericSchema schema = parentObject instanceof GenericSchema ? (GenericSchema)parentObject : null;
        GenericCatalog catalog = parentObject instanceof GenericCatalog ? (GenericCatalog)parentObject :
            schema == null ? null : schema.getCatalog();
        final GenericDataSource dataSource = getDataSource();
        DBPIdentifierCase convertCase = caseSensitive ? dataSource.getSQLDialect().storesQuotedCase() : dataSource.getSQLDialect().storesUnquotedCase();
        objectNameMask = convertCase.transform(objectNameMask);

        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(session, catalog, schema, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(session, catalog, schema, objectNameMask, maxResults, references);
        }
    }

    private void findTablesByMask(JDBCSession session, GenericCatalog catalog, GenericSchema schema, String tableNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        final GenericMetaObject tableObject = getDataSource().getMetaObject(GenericConstants.OBJECT_TABLE);
        final DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCResultSet dbResult = session.getMetaData().getTables(
            catalog == null ? null : catalog.getName(),
            schema == null ? null : schema.getName(),
            tableNameMask,
            null)) {
            while (dbResult.next()) {
                if (monitor.isCanceled()) {
                    break;
                }
                String catalogName = GenericUtils.safeGetStringTrimmed(tableObject, dbResult, JDBCConstants.TABLE_CAT);
                String schemaName = GenericUtils.safeGetStringTrimmed(tableObject, dbResult, JDBCConstants.TABLE_SCHEM);
                String tableName = GenericUtils.safeGetStringTrimmed(tableObject, dbResult, JDBCConstants.TABLE_NAME);
                if (CommonUtils.isEmpty(tableName)) {
                    continue;
                }
                objects.add(new TableReference(
                    findContainer(session.getProgressMonitor(), catalog, schema, catalogName, schemaName),
                    tableName,
                    GenericUtils.safeGetString(tableObject, dbResult, JDBCConstants.REMARKS)));
                if (objects.size() >= maxResults) {
                    break;
                }
            }
        }
    }

    private void findProceduresByMask(JDBCSession session, GenericCatalog catalog, GenericSchema schema, String procNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        final GenericMetaObject procObject = getDataSource().getMetaObject(GenericConstants.OBJECT_PROCEDURE);
        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCResultSet dbResult = session.getMetaData().getProcedures(
            catalog == null ? null : catalog.getName(),
            schema == null ? null : schema.getName(),
            procNameMask)) {
            while (dbResult.next()) {
                if (monitor.isCanceled()) {
                    break;
                }
                String catalogName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_CAT);
                String schemaName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_SCHEM);
                String procName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_NAME);
                String uniqueName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.SPECIFIC_NAME);
                if (CommonUtils.isEmpty(procName)) {
                    continue;
                }
                if (CommonUtils.isEmpty(uniqueName)) {
                    uniqueName = procName;
                }
                objects.add(new ProcedureReference(
                    findContainer(session.getProgressMonitor(), catalog, schema, catalogName, schemaName),
                    catalogName,
                    procName, uniqueName));
                if (objects.size() >= maxResults) {
                    break;
                }
            }
        }
    }

    protected GenericStructContainer findContainer(DBRProgressMonitor monitor, GenericCatalog parentCatalog, GenericSchema parentSchema, String catalogName, String schemaName) throws DBException
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
        return tableSchema != null ? tableSchema : tableCatalog != null ? tableCatalog : dataSource;
    }

    private abstract class ObjectReference extends AbstractObjectReference {

        protected ObjectReference(GenericStructContainer container, String name, String description, DBSObjectType type)
        {
            super(name, container, description, type);
        }

        @Override
        public GenericStructContainer getContainer()
        {
            return (GenericStructContainer)super.getContainer();
        }
    }

    private class TableReference extends ObjectReference {

        private TableReference(GenericStructContainer container, String tableName, String description)
        {
            super(container, tableName, description, RelationalObjectType.TYPE_TABLE);
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
        {
            GenericTable table = getContainer().getTable(monitor, getName());
            if (table == null) {
                throw new DBException("Can't find table '" + getName() + "' in '" + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
            }
            return table;
        }
    }

    private class ProcedureReference extends ObjectReference {

        private final String catalogName;
        private String uniqueName;
        private ProcedureReference(GenericStructContainer container, String catalogName, String procedureName, String uniqueName)
        {
            super(container, procedureName, null, RelationalObjectType.TYPE_PROCEDURE);
            this.catalogName = catalogName;
            this.uniqueName = uniqueName;
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
        {
            GenericProcedure procedure = null;
            if (getContainer() instanceof GenericSchema) {
                // Try to use catalog name as package name (Oracle)
                if (!CommonUtils.isEmpty(catalogName)) {
                    GenericPackage procPackage = ((GenericSchema)getContainer()).getPackage(monitor, catalogName);
                    if (procPackage != null) {
                        procedure = procPackage.getProcedure(monitor, uniqueName);
                    }
                }
            }
            if (procedure == null) {
                procedure = getContainer().getProcedure(monitor, uniqueName);
            }
            if (procedure == null) {
                throw new DBException("Can't find procedure '" + getName() + "' (" + uniqueName + ")" + "' in '" + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
            }
            return procedure;
        }
    }
}
