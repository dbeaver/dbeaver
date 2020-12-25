/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolVirtualSchema;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public class ExasolSchemaManager
    extends SQLObjectEditor<ExasolSchema, ExasolDataSource> implements DBEObjectRenamer<ExasolSchema> {


    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, ExasolSchema> getObjectsCache(
        ExasolSchema object) {
        ExasolDataSource source = object.getDataSource();
        return source.getSchemaCache();
    }

    @Override
    public boolean canCreateObject(Object container) {
        return super.canCreateObject(container);
    }

    @Override
    protected ExasolSchema createDatabaseObject(
        DBRProgressMonitor monitor,
        DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBCException {
        Object navContainer = options.get(DBEObjectMaker.OPTION_CONTAINER);
        boolean virtSchema = navContainer instanceof DBNDatabaseFolder && ((DBNDatabaseFolder) navContainer).getChildrenClass() == ExasolVirtualSchema.class;
        if (virtSchema) {
            throw new DBCFeatureNotSupportedException();
        }
        return new ExasolSchema((ExasolDataSource) container, "NEW_SCHEMA", "");
    }

    private void changeLimit(List<DBEPersistAction> actions, ExasolSchema schema, BigDecimal limit) {
        String script = String.format("ALTER SCHEMA %s SET RAW_SIZE_LIMIT = %d", DBUtils.getQuotedIdentifier(schema), limit.longValue());
        actions.add(
            new SQLDatabasePersistAction(ExasolMessages.manager_schema_raw_limit, script)
        );
    }

    private void changeOwner(List<DBEPersistAction> actions, ExasolSchema schema, String owner) {
        String script = "ALTER SCHEMA " + DBUtils.getQuotedIdentifier(schema) + " CHANGE OWNER  " + owner;
        actions.add(
            new SQLDatabasePersistAction(ExasolMessages.manager_schema_owner, script)
        );

    }


    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        final ExasolSchema schema = command.getObject();

        String script = "CREATE SCHEMA " + DBUtils.getQuotedIdentifier(schema);

        actions.add(
            new SQLDatabasePersistAction(ExasolMessages.manager_schema_create, script)
        );
        String owner = schema.getOwner();
        if (owner != null) {
            changeOwner(actions, schema, owner);
        }

        if (schema.getRawObjectSizeLimit() != null) {
            changeLimit(actions, schema, schema.getRawObjectSizeLimit());
        }
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
    	if (command.getObject() instanceof ExasolVirtualSchema)
    	{
            actions.add(
                    new SQLDatabasePersistAction("Drop schema", "DROP VIRTUAL SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject()) + " CASCADE") //$NON-NLS-2$
                );
    	} else {
            actions.add(
                    new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject()) + " CASCADE") //$NON-NLS-2$
                );
    		
    	}
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectRenameCommand command, Map<String, Object> options) {
        ExasolSchema obj = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename Schema",
                "RENAME SCHEMA " + DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getOldName()) + " to " +
                    DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getNewName()))
        );
    }

    @Override
    public void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
        ExasolSchema schema = command.getObject();

        if (command.getProperties().size() >= 1) {
            if (command.getProperties().containsKey(DBConstants.PROP_ID_DESCRIPTION)) {
                String script = "COMMENT ON SCHEMA " + DBUtils.getQuotedIdentifier(schema) + " IS '" + ExasolUtils.quoteString(CommonUtils.notNull(schema.getDescription(), "")) + "'";
                actionList.add(
                    new SQLDatabasePersistAction("Change comment on Schema", script)
                );
            }
            if (command.getProperties().containsKey("owner")) {
                changeOwner(actionList, schema, schema.getOwner());
            }

            if (command.getProperties().containsKey("rawObjectSizeLimit")) {
                changeLimit(actionList, schema, schema.getRawObjectSizeLimit());
            }

        }
    }

    @Override
    public void renameObject(DBECommandContext commandContext,
                             ExasolSchema object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }


}
