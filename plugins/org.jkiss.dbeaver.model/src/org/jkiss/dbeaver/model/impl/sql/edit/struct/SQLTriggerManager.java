/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * SQL trigger manager
 */
public abstract class SQLTriggerManager<OBJECT_TYPE extends DBSTrigger, CONTAINER_TYPE extends DBSObject>
    extends SQLObjectEditor<OBJECT_TYPE, CONTAINER_TYPE>
{

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Trigger name cannot be empty");
        }
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        createOrReplaceTriggerQuery(actions, command.getObject());
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        createOrReplaceTriggerQuery(actionList, command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop trigger",
                "DROP TRIGGER " + DBUtils.getObjectFullName(command.getObject(), DBPEvaluationContext.DDL))
        );
    }

    protected abstract void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, OBJECT_TYPE trigger);

}

