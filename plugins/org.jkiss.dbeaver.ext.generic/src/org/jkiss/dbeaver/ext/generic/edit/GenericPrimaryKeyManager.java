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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableCheckConstraint;

import java.util.List;
import java.util.Map;

/**
 * Generic constraint manager
 */
public class GenericPrimaryKeyManager extends SQLConstraintManager<GenericUniqueKey, GenericTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericUniqueKey> getObjectsCache(GenericUniqueKey object)
    {
        return object.getParentObject().getContainer().getConstraintKeysCache();
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return (container instanceof GenericTable)
            && (!(((GenericTable) container).getDataSource().getInfo() instanceof GenericDataSourceInfo) || ((GenericDataSourceInfo) ((GenericTable) container).getDataSource().getInfo()).supportsTableConstraints())
            && GenericUtils.canAlterTable((GenericTable) container);
    }

    @Override
    public boolean canEditObject(GenericUniqueKey object) {
        return GenericUtils.canAlterTable(object);
    }

    @Override
    public boolean canDeleteObject(@NotNull GenericUniqueKey object) {
        return GenericUtils.canAlterTable(object);
    }

    @Override
    protected GenericUniqueKey createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        final Object container,
        Object from,
        @NotNull Map<String, Object> options
    ) {
        GenericTableBase tableBase = (GenericTableBase)container;
        return tableBase.getDataSource().getMetaModel().createConstraintImpl(
            tableBase,
            GenericConstants.BASE_CONSTRAINT_NAME,
            DBSEntityConstraintType.PRIMARY_KEY,
            null,
            false);
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        GenericUniqueKey key = command.getObject();
        if (key.getConstraintType() == DBSEntityConstraintType.CHECK && (key instanceof DBSTableCheckConstraint check)) {
            actions.add(
                new SQLDatabasePersistAction(
                    ModelMessages.model_jdbc_create_new_constraint,
                    "ALTER TABLE " + key.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                        " ADD CONSTRAINT " + DBUtils.getQuotedIdentifier(key) + " CHECK (" + check.getCheckConstraintDefinition() + ")"
                ));
        } else {
            super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        }
    }

    @Override
    protected boolean isLegacyConstraintsSyntax(GenericTableBase owner) {
        return GenericUtils.isLegacySQLDialect(owner);
    }

    @Override
    protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<GenericUniqueKey> command) {
        GenericUniqueKey object = command.getObject();
        if (object.getConstraintType() == DBSEntityConstraintType.CHECK && (object instanceof DBSTableCheckConstraint check)) {
            decl.append("(").append(check.getCheckConstraintDefinition()).append(")");
        } else {
            super.appendConstraintDefinition(decl, command);
        }
    }
}
