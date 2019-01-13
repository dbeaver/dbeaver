/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableReal;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;
import java.util.Map;

/**
 * PostgreTriggerManager
 */
public class PostgreTriggerManager extends SQLObjectEditor<PostgreTrigger, PostgreTableReal> {//implements DBEObjectRenamer<PostgreTrigger> {

    @Override
    public boolean canCreateObject(PostgreTableReal parent) {
        return false;
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTrigger> getObjectsCache(PostgreTrigger object) {
        return object.getParentObject().getTriggerCache();
    }

    @Override
    protected PostgreTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, PostgreTableReal parent, Object copyFrom) throws DBException {
        return null;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {

    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, NestedObjectCommand<PostgreTrigger, PropertyHandler> command, Map<String, Object> options) throws DBException {
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment trigger",
                "COMMENT ON TRIGGER " + DBUtils.getQuotedIdentifier(command.getObject()) + " ON " + DBUtils.getQuotedIdentifier(command.getObject().getTable()) +
                    " IS " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription())));
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {

    }


}

