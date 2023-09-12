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

package org.jkiss.dbeaver.ext.dm.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DMDataSource;
import org.jkiss.dbeaver.ext.dm.model.DMSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * @author Shengkai Bai
 */
public class DMSchemaManager extends SQLObjectEditor<DMSchema, DMDataSource> {

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    public boolean canDeleteObject(DMSchema object) {
        return true;
    }

    @Override
    protected String getBaseObjectName() {
        return "NEW_SCHEMA";
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, DMSchema> getObjectsCache(DMSchema object) {
        return object.getDataSource().getSchemaCache();
    }


    @Override
    protected DMSchema createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        return new DMSchema((DMDataSource) container, null, "NEW_SCHEMA");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<DMSchema, DMDataSource>.ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        actions.add(
                new SQLDatabasePersistAction(
                        "Create schema",
                        "CREATE SCHEMA " + DBUtils.getObjectFullName(command.getObject(), DBPEvaluationContext.DDL))
        );
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<DMSchema, DMDataSource>.ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        actions.add(
                new SQLDatabasePersistAction(
                        "Drop schema",
                        "DROP SCHEMA " + DBUtils.getObjectFullName(command.getObject(), DBPEvaluationContext.DDL) + " RESTRICT")
        );
    }

}
