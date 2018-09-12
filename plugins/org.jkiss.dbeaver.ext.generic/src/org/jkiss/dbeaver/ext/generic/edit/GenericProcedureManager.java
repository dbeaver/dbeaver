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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.ListCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;
import java.util.Map;

/**
 * Generic procedure manager
 */
public class GenericProcedureManager extends SQLObjectEditor<GenericProcedure, GenericStructContainer> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericProcedure> getObjectsCache(GenericProcedure object)
    {
        GenericStructContainer container = object.getContainer();
        return new ListCache<>(((GenericObjectContainer) container).getProcedureCache());
    }

    @Override
    protected GenericProcedure createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final GenericStructContainer parent,
        Object from)
    {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        GenericProcedure object = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + object.getProcedureType().name() +  //$NON-NLS-2$
                    " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL))
        );
    }

    @Override
    public boolean canCreateObject(GenericStructContainer parent) {
        return false;
    }

    @Override
    public boolean canDeleteObject(GenericProcedure object) {
        return true;
    }

}
