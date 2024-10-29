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
package org.jkiss.dbeaver.ext.db2.i.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.i.model.DB2IConstraint;
import org.jkiss.dbeaver.ext.generic.edit.GenericPrimaryKeyManager;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.util.List;
import java.util.Map;

public class DB2IConstraintManager extends GenericPrimaryKeyManager {

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        GenericUniqueKey key = command.getObject();
        GenericStructContainer container = key.getParentObject().getParentObject();
        if (key.getConstraintType() == DBSEntityConstraintType.CHECK && key instanceof DB2IConstraint && container != null) {
            DB2IConstraint constraint = (DB2IConstraint) key;
            actions.add(
                new SQLDatabasePersistAction("Create check constraint",
                    "ALTER TABLE " + constraint.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                        " ADD CONSTRAINT " + DBUtils.getFullyQualifiedName(constraint.getDataSource(), container.getName(), constraint.getName()) +
                        " CHECK (" + constraint.getCheckConstraintDefinition() + ")"
                ));
        } else {
            super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        }
    }
}
