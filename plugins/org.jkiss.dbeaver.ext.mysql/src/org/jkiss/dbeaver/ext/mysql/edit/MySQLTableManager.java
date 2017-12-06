/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * MySQL table manager
 */
public class MySQLTableManager extends SQLTableManager<MySQLTableBase, MySQLCatalog> implements DBEObjectRenamer<MySQLTableBase> {

    private static final Class<?>[] CHILD_TYPES = {
        MySQLTableColumn.class,
        MySQLTableConstraint.class,
        MySQLTableForeignKey.class,
        MySQLTableIndex.class,
    };

    @Nullable
    @Override
    public DBSObjectCache<MySQLCatalog, MySQLTableBase> getObjectsCache(MySQLTableBase object)
    {
        return object.getContainer().getTableCache();
    }

    @Override
    protected MySQLTableBase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, MySQLCatalog parent, Object copyFrom) throws DBException
    {
        final MySQLTable table;
        if (copyFrom instanceof DBSEntity) {
            table = new MySQLTable(monitor, parent, (DBSEntity)copyFrom);
            table.setName(getNewChildName(monitor, parent, ((DBSEntity) copyFrom).getName()));
        } else if (copyFrom == null) {
            table = new MySQLTable(parent);
            setTableName(monitor, parent, table);

            final MySQLTable.AdditionalInfo additionalInfo = table.getAdditionalInfo(monitor);
            additionalInfo.setEngine(parent.getDataSource().getDefaultEngine());
            additionalInfo.setCharset(parent.getDefaultCharset());
            additionalInfo.setCollation(parent.getDefaultCollation());
        } else {
            throw new DBException("Can't create MySQL table from '" + copyFrom + "'");
        }

        return table;
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
        query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" "); //$NON-NLS-1$
        appendTableModifiers(command.getObject(), command, query);

        actionList.add(
            new SQLDatabasePersistAction(query.toString())
        );
    }

    @Override
    protected void addStructObjectCreateActions(List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) {

        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_OBJECT_DROP)) {
            final MySQLTableBase table = command.getObject();
            final String tableName = CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true) ?
                table.getFullyQualifiedName(DBPEvaluationContext.DDL) : DBUtils.getQuotedIdentifier(table);
            actions.add( 0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table, "DROP TABLE IF EXISTS " + table) );
        }

        super.addStructObjectCreateActions(actions, command, options);
    }

    @Override
    protected void appendTableModifiers(MySQLTableBase tableBase, NestedObjectCommand tableProps, StringBuilder ddl)
    {
        if (tableBase instanceof MySQLTable) {
            MySQLTable table =(MySQLTable)tableBase;
            try {
                final MySQLTable.AdditionalInfo additionalInfo = table.getAdditionalInfo(new VoidProgressMonitor());
                if ((!table.isPersisted() || tableProps.getProperty("engine") != null) && additionalInfo.getEngine() != null) { //$NON-NLS-1$
                    ddl.append("\nENGINE=").append(additionalInfo.getEngine().getName()); //$NON-NLS-1$
                }
                if ((!table.isPersisted() || tableProps.getProperty("charset") != null) && additionalInfo.getCharset() != null) { //$NON-NLS-1$
                    ddl.append("\nDEFAULT CHARSET=").append(additionalInfo.getCharset().getName()); //$NON-NLS-1$
                }
                if ((!table.isPersisted() || tableProps.getProperty("collation") != null) && additionalInfo.getCollation() != null) { //$NON-NLS-1$
                    ddl.append("\nCOLLATE=").append(additionalInfo.getCollation().getName()); //$NON-NLS-1$
                }
                if ((!table.isPersisted() || tableProps.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) && table.getDescription() != null) {
                    ddl.append("\nCOMMENT=").append(SQLUtils.quoteString(table, table.getDescription())); //$NON-NLS-1$ //$NON-NLS-2$
                }
                if ((!table.isPersisted() || tableProps.getProperty("autoIncrement") != null) && additionalInfo.getAutoIncrement() > 0) { //$NON-NLS-1$
                    ddl.append("\nAUTO_INCREMENT=").append(additionalInfo.getAutoIncrement()); //$NON-NLS-1$
                }
            } catch (DBCException e) {
                log.error(e);
            }
        }
    }

    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final MySQLDataSource dataSource = command.getObject().getDataSource();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "RENAME TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-1$
                    " TO " + DBUtils.getQuotedIdentifier(command.getObject().getContainer()) + "." + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())) //$NON-NLS-1$
        );
    }

    @NotNull
    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    public Collection<? extends DBSObject> getChildObjects(DBRProgressMonitor monitor, MySQLTableBase object, Class<? extends DBSObject> childType) throws DBException {
        if (childType == MySQLTableColumn.class) {
            return object.getAttributes(monitor);
        } else if (childType == MySQLTableConstraint.class) {
            return object.getConstraints(monitor);
        } else if (childType == MySQLTableForeignKey.class) {
            return object.getAssociations(monitor);
        } else if (childType == MySQLTableIndex.class) {
            return object.getIndexes(monitor);
        }
        return null;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, MySQLTableBase object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }

}
