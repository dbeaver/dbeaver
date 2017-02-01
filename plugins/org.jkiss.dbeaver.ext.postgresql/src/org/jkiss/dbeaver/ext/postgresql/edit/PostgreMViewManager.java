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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreMaterializedView;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreViewBase;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * PostgreViewManager
 */
public class PostgreMViewManager extends PostgreViewManager {

    @Override
    protected PostgreMaterializedView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, PostgreSchema parent, Object copyFrom)
    {
        PostgreMaterializedView newMV = new PostgreMaterializedView(parent);
        newMV.setName("new_mview"); //$NON-NLS-1$
        return newMV;
    }

    @Override
    protected void createOrReplaceViewQuery(List<DBEPersistAction> actions, PostgreViewBase view)
    {
        super.createOrReplaceViewQuery(actions, view);
        // Indexes DDL
    }

}

