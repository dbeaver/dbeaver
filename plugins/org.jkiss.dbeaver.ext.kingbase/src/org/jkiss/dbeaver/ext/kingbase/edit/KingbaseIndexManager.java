/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseIndex;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseIndexColumn;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseOperatorClass;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableBase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
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

/**
 * Kingbase index manager
 */
public class KingbaseIndexManager extends SQLIndexManager<KingbaseIndex, KingbaseTableBase> implements DBEObjectRenamer<KingbaseIndex> {

    @Override
    public boolean canRenameObject(KingbaseIndex object) {
        return true;
    }

    @Nullable
    @Override
    public DBSObjectCache<KingbaseTableContainer, KingbaseIndex> getObjectsCache(KingbaseIndex object)
    {
        return object.getTable().getContainer().getSchema().getIndexCache();
    }

    @Override
    protected KingbaseIndex createDatabaseObject(
        @NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container,
        Object from, @NotNull Map<String, Object> options)
    {
        KingbaseTableBase tableBase = (KingbaseTableBase) container;
        return new KingbaseIndex(
            tableBase,
            "NewIndex",
            DBSIndexType.UNKNOWN,
            false);
    }

    protected void appendIndexColumnModifiers(DBRProgressMonitor monitor, StringBuilder decl, DBSTableIndexColumn indexColumn) {
        try {
            final KingbaseOperatorClass operatorClass = ((KingbaseIndexColumn) indexColumn).getOperatorClass(monitor);
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
    public void deleteObject(@NotNull DBECommandContext commandContext, @NotNull KingbaseIndex object, @NotNull Map<String, Object> options) throws DBException {
        if (object.isPrimaryKeyIndex()) {
            throw new DBException("You can not drop constraint-based unique index.\n" +
                "Try to drop constraint '" + object.getName() + "'.");
        }
        super.deleteObject(commandContext, object, options);
    }

    @Override
    protected String getDropIndexPattern(KingbaseIndex index)
    {
        return "DROP INDEX " + PATTERN_ITEM_INDEX; //$NON-NLS-1$
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        boolean hasDDL = false;
        KingbaseIndex index = command.getObject();
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

    private static void addIndexCommentAction(List<DBEPersistAction> actions, KingbaseIndex index) {
        actions.add(new SQLDatabasePersistAction(
            "Comment index",
            "COMMENT ON INDEX " + index.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                " IS " + SQLUtils.quoteString(index, index.getDescription())));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull KingbaseIndex object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        KingbaseIndex index = command.getObject();
        KingbaseDataSource dataSource = index.getDataSource();
        actions.add(
                new SQLDatabasePersistAction(
                        "Rename index",
                        "ALTER INDEX " + DBUtils.getQuotedIdentifier(index.getTable().getContainer()) + "." + //$NON-NLS-1$
                                DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) +
                                " RENAME TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())) //$NON-NLS-1$
        );
    }
}
