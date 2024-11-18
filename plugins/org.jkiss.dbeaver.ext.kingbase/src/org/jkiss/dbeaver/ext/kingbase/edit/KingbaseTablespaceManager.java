/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.kingbase.edit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDatabase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTablespace;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;


public class KingbaseTablespaceManager extends SQLObjectEditor<KingbaseTablespace, KingbaseDatabase> {

    private final static Set<String> systemTablespaces = new HashSet<>(Arrays.asList("sys_default", "sys_default", "sys_global", "sys_global"));

    private static final Log log = Log.getLog(KingbaseTablespaceManager.class);

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<KingbaseDatabase, KingbaseTablespace> getObjectsCache(KingbaseTablespace object) {
        return object.getDatabase().tablespaceCache;
    }

    @Override
    protected KingbaseTablespace createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        Object container,
        Object copyFrom,
        @NotNull Map<String, Object> options) throws DBException
    {
        return new KingbaseTablespace((KingbaseDatabase) container);
    }

    @Override
    public void deleteObject(@NotNull DBECommandContext commandContext, @NotNull KingbaseTablespace object, @NotNull Map<String, Object> options)
        throws DBException {
        if (systemTablespaces.contains(object.getName().toLowerCase())) {
            DBWorkbench.getPlatformUI().showError("Drop tablespace", "Unable to drop system tablespace " + object.getName());
        } else {
            super.deleteObject(commandContext, object, options);
        }
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options) {
        final KingbaseTablespace tablespace = command.getObject();

        try {
            actions.add(
                new SQLDatabasePersistActionAtomic("Create tablespace", tablespace.getObjectDefinitionText(monitor, options)) //$NON-NLS-2$
            );
        } catch (DBException e) {
            log.error(e);
        }
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options) {
        actions.add(
            new SQLDatabasePersistActionAtomic("Drop tablespace", "DROP TABLESPACE " //$NON-NLS-2$
                + DBUtils.getQuotedIdentifier(command.getObject()))
        );

    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return true;
    }

    @Override
    public boolean canDeleteObject(@NotNull KingbaseTablespace object) {
        return true;
    }

    @Override
    public boolean canEditObject(KingbaseTablespace object) {
        return false;
    }
}
