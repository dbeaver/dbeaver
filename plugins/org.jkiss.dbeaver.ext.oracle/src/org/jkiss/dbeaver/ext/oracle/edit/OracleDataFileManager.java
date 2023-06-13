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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataFile;
import org.jkiss.dbeaver.ext.oracle.model.OracleTablespace;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class OracleDataFileManager extends SQLObjectEditor<OracleDataFile, OracleTablespace> {

    @Override
    protected OracleDataFile createDatabaseObject(
        DBRProgressMonitor monitor,
        DBECommandContext context,
        Object container,
        Object copyFrom, Map<String,
        Object> options
    ) {
        if (container instanceof OracleTablespace) {
            return new OracleDataFile((OracleTablespace) container, "new_file");
        }
        return null;
    }

    @Override
    protected void addObjectCreateActions(
        DBRProgressMonitor monitor,
        DBCExecutionContext executionContext,
        List<DBEPersistAction> actions,
        ObjectCreateCommand command,
        Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Add data file", buildStatement(command.getObject())));
    }

    @Override
    protected void addObjectDeleteActions(
        DBRProgressMonitor monitor,
        DBCExecutionContext executionContext,
        List<DBEPersistAction> actions,
        ObjectDeleteCommand command,
        Map<String, Object> options
    ) {
        OracleDataFile dataFile = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Drop data file",
                "ALTER TABLESPACE " + DBUtils.getQuotedIdentifier(dataFile.getTablespace()) +
                    " DROP " + (dataFile.isTemporary() ? "TEMPFILE " : "DATAFILE ") +
                    SQLUtils.quoteString(dataFile.getDataSource(), dataFile.getName()))
        );
    }

    private String buildStatement(@NotNull OracleDataFile dataFile) {
        StringBuilder sb = new StringBuilder("ALTER TABLESPACE ");
        sb.append(DBUtils.getQuotedIdentifier(dataFile.getTablespace()));
        sb.append(" ADD ");
        if (dataFile.isTemporary()) {
            sb.append("TEMPFILE ");
        } else {
            sb.append("DATAFILE ");
        }
        sb.append(SQLUtils.quoteString(dataFile.getDataSource(), dataFile.getName()));
        sb.append(" SIZE ");
        if (dataFile.getBytes() != null) {
            sb.append(dataFile.getBytes());
        } else {
            // Default value
            sb.append("10M");
        }
        if (dataFile.isAutoExtensible()) {
            sb.append(" AUTOEXTEND ON ");
            if (dataFile.getMaxBytes() != null) {
                sb.append("MAXSIZE ").append(dataFile.getMaxBytes());
            }
        }
        return sb.toString();
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleDataFile> getObjectsCache(OracleDataFile object) {
        return object.getTablespace().getFileCache();
    }
}
