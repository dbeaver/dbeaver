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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Postgre table manager
 */
public class PostgrePartitionManager extends SQLStructEditor<PostgreTablePartition, PostgreTableRegular> {

    @Override
    public boolean canCreateObject(PostgreTableRegular parent) {
        return false;
    }

    @Override
    public boolean canDeleteObject(PostgreTablePartition object) {
        return false;
    }

    @Override
    protected PostgreTablePartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, PostgreTableRegular parent, Object copyFrom) throws DBException {
        return null;
    }

    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {

    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {

    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {

    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return 0;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTablePartition> getObjectsCache(PostgreTablePartition object) {
        return null;
    }

    @Override
    public Class<?>[] getChildTypes() {
        return new Class[0];
    }

}
