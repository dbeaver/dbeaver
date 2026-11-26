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
package org.jkiss.dbeaver.ext.firebird.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdConstants;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdSequence;
import org.jkiss.dbeaver.ext.generic.edit.GenericSequenceManager;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Firebird sequence manager.
 * Dropping and commenting is inherited from the generic manager.
 */
public class FireBirdSequenceManager extends GenericSequenceManager {

    private static final String PROP_INCREMENT_BY = "incrementBy";

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        // The start value can't be changed later, so it is entered in the editor before the sequence is created
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return container instanceof GenericStructContainer;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericSequence> getObjectsCache(GenericSequence object) {
        // Firebird has no catalogs and schemas, sequences belong to the data source itself
        if (object.getParentObject() instanceof GenericStructContainer container) {
            return container.getSequenceCache();
        }
        return null;
    }

    @Override
    protected GenericSequence createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) throws DBException {
        GenericStructContainer structContainer = (GenericStructContainer) container;
        return new FireBirdSequence(structContainer, getNewChildName(monitor, structContainer));
    }

    @Override
    protected void validateObjectProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Sequence name cannot be empty");
        }
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        FireBirdSequence sequence = (FireBirdSequence) command.getObject();
        StringBuilder sql = new StringBuilder("CREATE SEQUENCE ")
            .append(sequence.getFullyQualifiedName(DBPEvaluationContext.DDL));
        // Default values are omitted: Firebird 2.5 does not support these clauses, and Firebird 3 treats START WITH
        // as the current value, so an explicit START WITH 1 would make the sequence start at 2 there
        Long startValue = sequence.getStartValue();
        if (startValue != null && startValue != FireBirdConstants.SEQUENCE_DEFAULT_START_VALUE) {
            sql.append(" START WITH ").append(startValue);
        }
        Long incrementBy = sequence.getIncrementBy();
        if (incrementBy != null && incrementBy != FireBirdConstants.SEQUENCE_DEFAULT_INCREMENT) {
            sql.append(" INCREMENT BY ").append(incrementBy);
        }
        actions.add(new SQLDatabasePersistAction("Create sequence", sql.toString()));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        FireBirdSequence sequence = (FireBirdSequence) command.getObject();
        if (command.hasProperty(PROP_INCREMENT_BY) && sequence.getIncrementBy() != null) {
            actions.add(new SQLDatabasePersistAction(
                "Alter sequence increment",
                "ALTER SEQUENCE " + sequence.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " INCREMENT BY " + sequence.getIncrementBy()));
        }
    }
}
