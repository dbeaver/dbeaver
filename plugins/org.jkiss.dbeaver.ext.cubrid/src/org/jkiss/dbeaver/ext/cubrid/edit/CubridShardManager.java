/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridShard;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;

public class CubridShardManager extends SQLObjectEditor<CubridShard, GenericStructContainer> {

    @Override
    protected void addObjectModifyActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectChangeCommand command,
            @NotNull Map<String, Object> options) {
        CubridShard shard = command.getObject();
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue("shardType", shard.getName());
        store.setValue("shardVal", shard.getValue());
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return false;
    }

    @Override
    public boolean canDeleteObject(@NotNull CubridShard object) {
    	return false;
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return 0;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, CubridShard> getObjectsCache(CubridShard object) {
        return null;
    }

    @Override
    protected CubridShard createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
            Object copyFrom, Map<String, Object> options) throws DBException {
        return null;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
            List<DBEPersistAction> actions,
            SQLObjectEditor<CubridShard, GenericStructContainer>.ObjectCreateCommand command,
            Map<String, Object> options) throws DBException {
        /* This body intentionally empty. */
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
            List<DBEPersistAction> actions,
            SQLObjectEditor<CubridShard, GenericStructContainer>.ObjectDeleteCommand command,
            Map<String, Object> options) throws DBException {
        /* This body intentionally empty. */
    }
}
