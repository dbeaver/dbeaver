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

package org.jkiss.dbeaver.ext.gbase8s.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.gbase8s.GBase8sUtils;
import org.jkiss.dbeaver.ext.generic.edit.GenericPrimaryKeyManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableCheckConstraint;

/**
 * @author Chao Tian
 */
public class GBase8sUniqueKeyManager extends GenericPrimaryKeyManager {

    @Override
    protected void addObjectCreateActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectCreateCommand command,
            @NotNull Map<String, Object> options) {
        GenericUniqueKey key = command.getObject();
        // Legacy Syntax for CHECK Constraints
        if (key.getConstraintType() == DBSEntityConstraintType.CHECK && (key instanceof DBSTableCheckConstraint check)
                && !GBase8sUtils.isOracleSqlMode(executionContext.getDataSource().getContainer())) {
            String createSql = "ALTER TABLE %s ADD CONSTRAINT CHECK (%s) CONSTRAINT %s".formatted(
                    key.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL),
                    check.getCheckConstraintDefinition(), DBUtils.getQuotedIdentifier(key));
            actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_constraint, createSql));
        } else {
            super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        }
    }

    @Override
    protected boolean isLegacyConstraintsSyntax(GenericTableBase owner) {
        return GBase8sUtils.isOracleSqlMode(owner.getContainer().getDataSource().getContainer()) ? false : true;
    }

    @Override
    protected boolean isShortNotation(GenericTableBase owner) {
        return false;
    }
}
