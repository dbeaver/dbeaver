/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
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

