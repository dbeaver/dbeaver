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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.cache.ListCache;

import java.util.List;
import java.util.Map;

public class GenericTriggerManager extends SQLTriggerManager<GenericTrigger, GenericTableBase> {

    @Override
    public boolean canCreateObject(Object container) {
        return false;
    }

    @Override
    public boolean canDeleteObject(GenericTrigger object) {
        return true;
    }

    @Override
    protected void createOrReplaceTriggerQuery(
        DBRProgressMonitor monitor,
        DBCExecutionContext executionContext,
        List<DBEPersistAction> actions,
        GenericTrigger trigger,
        boolean create
    ) {

    }

    @Override
    protected GenericTableTrigger createDatabaseObject(
        DBRProgressMonitor monitor,
        DBECommandContext context,
        Object container,
        Object copyFrom,
        Map<String, Object> options
    ) throws DBException {
        throw new IllegalStateException("Not implemented");
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTrigger> getObjectsCache(GenericTrigger object) {
        DBSObject container = object.getContainer();
        if (container instanceof GenericStructContainer) {
            return ((GenericStructContainer) container).getTableTriggerCache();
        } else if (container instanceof GenericTableBase) {
            return ((GenericTableBase) container).getContainer().getTableTriggerCache();
        }
        return null;
    }
}
