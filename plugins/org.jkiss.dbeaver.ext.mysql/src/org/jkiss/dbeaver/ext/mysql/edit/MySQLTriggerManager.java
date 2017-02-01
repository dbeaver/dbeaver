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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
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

/**
 * MySQLTriggerManager
 */
public class MySQLTriggerManager extends SQLTriggerManager<MySQLTrigger, MySQLTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MySQLTrigger> getObjectsCache(MySQLTrigger object)
    {
        return object.getCatalog().getTriggerCache();
    }

    @Override
    protected MySQLTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final MySQLTable parent, Object copyFrom)
    {
        return new UITask<MySQLTrigger>() {
            @Override
            protected MySQLTrigger runTask() {
                EntityEditPage editPage = new EntityEditPage(parent.getDataSource(), DBSEntityType.TRIGGER);
                if (!editPage.edit()) {
                    return null;
                }
                MySQLTrigger newTrigger = new MySQLTrigger(parent.getContainer(), parent, editPage.getEntityName());
                newTrigger.setObjectDefinitionText(""); //$NON-NLS-1$
                return newTrigger;
            }
        }.execute();
    }

    protected void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, MySQLTrigger trigger) {
        if (trigger.isPersisted()) {
            actions.add(
                new SQLDatabasePersistAction("Drop trigger",
                    "DROP TRIGGER IF EXISTS " + trigger.getFullyQualifiedName(DBPEvaluationContext.DDL))
            );
        }
        String ddl =
            "CREATE TRIGGER " + trigger.getFullyQualifiedName(DBPEvaluationContext.DDL) + "\n" +
                trigger.getActionTiming() + " " + trigger.getManipulationType() + "\n" +
            "ON " + trigger.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " FOR EACH ROW\n" +
            trigger.getBody();

        actions.add(new SQLDatabasePersistAction("Create trigger", ddl, true)); //$NON-NLS-2$
    }

}

