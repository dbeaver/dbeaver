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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableTrigger;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.List;
import java.util.Map;

/**
 * OracleTableTriggerManager
 */
public class OracleTableTriggerManager extends SQLTriggerManager<OracleTableTrigger, OracleTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTableTrigger> getObjectsCache(OracleTableTrigger object)
    {
        return object.getTable().triggerCache;
    }

    @Override
    protected OracleTableTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final OracleTableBase parent, Object copyFrom)
    {
        return new UITask<OracleTableTrigger>() {
            @Override
            protected OracleTableTrigger runTask() {
                EntityEditPage editPage = new EntityEditPage(parent.getDataSource(), DBSEntityType.TRIGGER);
                if (!editPage.edit()) {
                    return null;
                }
                OracleTableTrigger newTrigger = new OracleTableTrigger(parent, editPage.getEntityName());
                newTrigger.setObjectDefinitionText("TRIGGER " + editPage.getEntityName() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                    "BEGIN\n" + //$NON-NLS-1$
                    "END;"); //$NON-NLS-1$
                return newTrigger;
            }
        }.execute();
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop trigger", "DROP TRIGGER " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    protected void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, OracleTableTrigger trigger)
    {
        String source = OracleUtils.normalizeSourceName(trigger, false);
        if (source == null) {
            return;
        }
        actions.add(new SQLDatabasePersistAction("Create trigger", "CREATE OR REPLACE " + source, true)); //$NON-NLS-2$
        OracleUtils.addSchemaChangeActions(actions, trigger);
    }

}

