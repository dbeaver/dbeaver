/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.mysql.model.MySQLDatabase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
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
                newTrigger.setObjectDefinitionText(
                    "CREATE TRIGGER " + DBUtils.getQuotedIdentifier(newTrigger) + "\n" +
                    newTrigger.getActionTiming() + " " + newTrigger.getManipulationType() + "\n" +
                    "ON " + DBUtils.getQuotedIdentifier(parent) + " FOR EACH ROW\n");
                return newTrigger;
            }
        }.execute();
    }

    protected void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, MySQLTrigger trigger, boolean create) {
        if (trigger.isPersisted()) {
            actions.add(
                new SQLDatabasePersistAction("Drop trigger",
                    "DROP TRIGGER IF EXISTS " + trigger.getFullyQualifiedName(DBPEvaluationContext.DDL))
            );
        }
        MySQLDatabase curCatalog = trigger.getCatalog().getDataSource().getDefaultObject();
        if (curCatalog != trigger.getCatalog()) {
            actions.add(new SQLDatabasePersistAction("Set current schema ", "USE " + DBUtils.getQuotedIdentifier(trigger.getCatalog()), false)); //$NON-NLS-2$
        }

        actions.add(new SQLDatabasePersistAction("Create trigger", trigger.getBody(), true)); //$NON-NLS-2$

        if (curCatalog != null && curCatalog != trigger.getCatalog()) {
            actions.add(new SQLDatabasePersistAction("Set current schema ", "USE " + DBUtils.getQuotedIdentifier(curCatalog), false)); //$NON-NLS-2$
        }
    }

}

