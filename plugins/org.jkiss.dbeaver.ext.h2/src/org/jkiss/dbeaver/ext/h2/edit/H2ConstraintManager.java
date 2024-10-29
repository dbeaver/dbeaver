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
package org.jkiss.dbeaver.ext.h2.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericPrimaryKeyManager;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.ext.h2.model.H2Constraint;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class H2ConstraintManager extends GenericPrimaryKeyManager {

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        GenericUniqueKey key = command.getObject();
        GenericStructContainer container = key.getParentObject().getParentObject();
        if (key.getConstraintType() == DBSEntityConstraintType.CHECK && key instanceof H2Constraint && container != null) {
            H2Constraint constraint = (H2Constraint) key;
            actions.add(
                new SQLDatabasePersistAction("Create check constraint",
                    "ALTER TABLE " + constraint.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                        " ADD CONSTRAINT " + DBUtils.getFullyQualifiedName(constraint.getDataSource(), container.getName(), constraint.getName()) + " CHECK (" + constraint.getCheckConstraintDefinition() + ") NOCHECK"
                ));
        } else {
            super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        }
    }

    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, GenericTableBase owner, DBECommandAbstract<GenericUniqueKey> command, Map<String, Object> options) {
        GenericUniqueKey constraint = command.getObject();
        if (!constraint.isPersisted() && constraint.getConstraintType() == DBSEntityConstraintType.CHECK) {
            StringBuilder decl = new StringBuilder(40);
            decl.append(getAddConstraintTypeClause(constraint));
            appendConstraintDefinition(decl, command);
            if (!CommonUtils.isEmpty(decl)) {
                return decl;
            }
        }
        return super.getNestedDeclaration(monitor, owner, command, options);
    }

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<GenericUniqueKey, PropertyHandler> command, @NotNull Map<String, Object> options) throws DBException {
        GenericUniqueKey constraint = command.getObject();
        GenericStructContainer container = constraint.getParentObject().getParentObject();
        if (container != null && command.getProperties().containsKey(DBConstants.PROP_ID_DESCRIPTION)) {
            actions.add(
                new SQLDatabasePersistAction("Alter constraint description",
                    "COMMENT ON CONSTRAINT " + DBUtils.getFullyQualifiedName(constraint.getDataSource(), container.getName(), constraint.getName()) + " IS " +
                        SQLUtils.quoteString(constraint, CommonUtils.notEmpty(constraint.getDescription()))
                )
            );
        }
    }
}
