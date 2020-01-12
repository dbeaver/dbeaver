/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Postgre index manager
 */
public class PostgreIndexManager extends SQLIndexManager<PostgreIndex, PostgreTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<PostgreTableContainer, PostgreIndex> getObjectsCache(PostgreIndex object)
    {
        return object.getTable().getContainer().getSchema().indexCache;
    }

    @Override
    protected PostgreIndex createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        PostgreTableBase tableBase = (PostgreTableBase) container;
        return new PostgreIndex(
            tableBase,
            "NewIndex",
            DBSIndexType.UNKNOWN,
            false);
    }

    protected void appendIndexColumnModifiers(DBRProgressMonitor monitor, StringBuilder decl, DBSTableIndexColumn indexColumn) {
        try {
            final PostgreOperatorClass operatorClass = ((PostgreIndexColumn) indexColumn).getOperatorClass(monitor);
            if (operatorClass != null) {
                decl.append(" ").append(operatorClass.getName());
            }
        } catch (DBException e) {
            log.warn(e);
        }
        if (!indexColumn.isAscending()) {
            decl.append(" DESC"); //$NON-NLS-1$
        }
    }

    @Override
    protected String getDropIndexPattern(PostgreIndex index)
    {
        return "DROP INDEX " + PATTERN_ITEM_INDEX; //$NON-NLS-1$
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        boolean hasDDL = false;
        PostgreIndex index = command.getObject();
        if (index.isPersisted()) {
            try {
                String indexDDL = index.getObjectDefinitionText(monitor, DBPScriptObject.EMPTY_OPTIONS);
                if (!CommonUtils.isEmpty(indexDDL)) {
                    actions.add(
                        new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, indexDDL)
                    );
                    hasDDL = true;
                }
            } catch (DBException e) {
                log.warn("Can't extract index DDL", e);
            }
        }
        if (!hasDDL) {
            super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        }
        if (!CommonUtils.isEmpty(index.getDescription())) {
            addIndexCommentAction(actions, index);
        }
    }

    static void addIndexCommentAction(List<DBEPersistAction> actions, PostgreIndex index) {
        actions.add(new SQLDatabasePersistAction(
            "Comment index",
            "COMMENT ON INDEX " + index.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                " IS " + SQLUtils.quoteString(index, index.getDescription())));
    }
}
