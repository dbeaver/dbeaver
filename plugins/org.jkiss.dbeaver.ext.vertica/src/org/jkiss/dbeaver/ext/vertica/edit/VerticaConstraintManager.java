/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.vertica.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericPrimaryKeyManager;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.ext.vertica.model.VerticaConstraint;
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

public class VerticaConstraintManager extends GenericPrimaryKeyManager {

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        VerticaConstraint constraint = (VerticaConstraint) command.getObject();
        if (command.getObject().getConstraintType() == DBSEntityConstraintType.CHECK) {
            actions.add(
                    new SQLDatabasePersistAction("Create check constraint", 
                        "ALTER TABLE " + constraint.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                            " ADD CONSTRAINT " + DBUtils.getQuotedIdentifier(constraint) + " CHECK (" + constraint.getCheckConstraintDefinition() + ")"
                    ));
        } else {
            super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        }
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        VerticaConstraint constraint = (VerticaConstraint) command.getObject();

        if (command.getProperties().containsKey(DBConstants.PROP_ID_ENABLED)) {
            actionList.add(
                new SQLDatabasePersistAction("Alter constraint",
                    "ALTER TABLE " + constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                        " ALTER CONSTRAINT " + DBUtils.getQuotedIdentifier(constraint.getDataSource(), constraint.getName()) + " " +
                        (constraint.isEnabled() ? "ENABLED" : "DISABLED")
                    )
            );
        }

        if (command.getProperties().containsKey(DBConstants.PROP_ID_DESCRIPTION)) {
            actionList.add(
                new SQLDatabasePersistAction("Alter constraint description",
                    "COMMENT ON CONSTRAINT " + DBUtils.getQuotedIdentifier(constraint.getDataSource(), constraint.getName()) +
                        " ON " + constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS " +
                        SQLUtils.quoteString(constraint, CommonUtils.notEmpty(constraint.getDescription()))
                    )
            );
        }
        super.addObjectModifyActions(monitor, executionContext, actionList, command, options);
    }

    @Override
    protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<GenericUniqueKey> command) {
        if (command.getObject().getConstraintType() == DBSEntityConstraintType.CHECK) {
            decl.append("(").append(((VerticaConstraint)command.getObject()).getCheckConstraintDefinition()).append(")");
        } else {
            super.appendConstraintDefinition(decl, command);
        }
    }
}
