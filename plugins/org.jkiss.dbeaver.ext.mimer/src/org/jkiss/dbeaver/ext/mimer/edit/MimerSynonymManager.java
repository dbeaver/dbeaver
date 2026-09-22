/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mimer.model.MimerSchema;
import org.jkiss.dbeaver.ext.mimer.model.MimerSynonym;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * Adds CREATE / DROP support for Mimer SQL synonyms, so "Create New Synonym" is available on
 * the Synonyms tree node. No ALTER support beyond Comment - Mimer SQL has no way to retarget or
 * rename an existing synonym, only drop and recreate.
 *
 * @author Mimer Information Technology
 */
public class MimerSynonymManager extends SQLObjectEditor<MimerSynonym, MimerSchema> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerSynonym object) {
        return true;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public DBSObjectCache<? extends DBSObject, MimerSynonym> getObjectsCache(MimerSynonym object) {
        // GenericObjectContainer#getSynonymCache() is parameterized on the base GenericSynonym,
        // not MimerSynonym - safe to narrow since MimerMetaModel#createSynonymImpl is the only
        // thing that ever populates it, and it only ever constructs MimerSynonym instances.
        return (DBSObjectCache<? extends DBSObject, MimerSynonym>) (DBSObjectCache<?, ?>)
            ((MimerSchema) object.getParentObject()).getSynonymCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_SYNONYM";
    }

    @Override
    protected MimerSynonym createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerSynonym((MimerSchema) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerSynonym synonym = command.getObject();
        actions.add(new SQLDatabasePersistAction("Create synonym", synonym.getObjectDefinitionText(monitor, options)));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerSynonym synonym = command.getObject();
        // DROP SYNONYM takes no RESTRICT/CASCADE clause (matches the DbVisualizer Mimer profile's
        // own drop action, which offers none) - plain drop only.
        actions.add(new SQLDatabasePersistAction("Drop synonym",
            "DROP SYNONYM \"" + synonym.getParentObject().getName() + "\".\"" + synonym.getName() + "\""));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (!command.hasProperty("comment")) {
            return;
        }
        MimerSynonym synonym = command.getObject();
        String name = "\"" + synonym.getParentObject().getName() + "\".\"" + synonym.getName() + "\"";
        actionList.add(new SQLDatabasePersistAction("Comment synonym",
            MimerUtils.buildCommentDDL(synonym, "SYNONYM", name, synonym.getComment(monitor))));
    }
}
