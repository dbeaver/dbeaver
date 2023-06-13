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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleTablespace;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class OracleTablespaceManager extends SQLObjectEditor<OracleTablespace, OracleDataSource> {

    @Override
    public boolean canCreateObject(Object container) {
        return false;
    }

    @Override
    protected OracleTablespace createDatabaseObject(
        DBRProgressMonitor monitor,
        DBECommandContext context,
        Object container,
        Object copyFrom,
        Map<String, Object> options
    ) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    protected void addObjectCreateActions(
        DBRProgressMonitor monitor,
        DBCExecutionContext executionContext,
        List<DBEPersistAction> actions,
        ObjectCreateCommand command,
        Map<String, Object> options
    ) {

    }

    @Override
    protected void addObjectDeleteActions(
        DBRProgressMonitor monitor,
        DBCExecutionContext executionContext,
        List<DBEPersistAction> actions,
        ObjectDeleteCommand command,
        Map<String, Object> options
    ) {
        actions.add(
            new SQLDatabasePersistAction(
                "Drop tablespace",
                "DROP TABLESPACE " + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-1$
        );
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTablespace> getObjectsCache(OracleTablespace object) {
        return object.getDataSource().getTablespaceCache();
    }

    // For the future. Now only one updatable property looks terrible
    /*@Override
    protected void addObjectRenameActions(
        DBRProgressMonitor monitor,
        DBCExecutionContext executionContext,
        List<DBEPersistAction> actions,
        ObjectRenameCommand command,
        Map<String, Object> options
    ) {
        OracleDataSource dataSource = command.getObject().getDataSource();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename tablespace",
                "ALTER TABLESPACE " + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) + //$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())) //$NON-NLS-1$
        );
    }*/
}
