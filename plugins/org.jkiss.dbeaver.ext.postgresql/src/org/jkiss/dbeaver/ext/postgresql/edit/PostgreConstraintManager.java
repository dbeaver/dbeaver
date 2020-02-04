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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableConstraint;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableConstraintBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableContainer;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Postgre constraint manager
 */
public class PostgreConstraintManager extends SQLConstraintManager<PostgreTableConstraintBase, PostgreTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<PostgreTableContainer, PostgreTableConstraintBase> getObjectsCache(PostgreTableConstraintBase object)
    {
        return object.getTable().getContainer().getSchema().constraintCache;
    }

    @Override
    protected PostgreTableConstraintBase createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        return new PostgreTableConstraint((PostgreTableBase) container, "NewConstraint", DBSEntityConstraintType.UNIQUE_KEY);
    }

    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, PostgreTableBase owner, DBECommandAbstract<PostgreTableConstraintBase> command, Map<String, Object> options) {
        PostgreTableConstraintBase constr = command.getObject();
        if (constr.isPersisted()) {
            try {
                String constrDDL = constr.getObjectDefinitionText(
                    monitor,
                    Collections.singletonMap(DBPScriptObject.OPTION_EMBEDDED_SOURCE, true));
                if (!CommonUtils.isEmpty(constrDDL)) {
                    return new StringBuilder(constrDDL);
                }
            } catch (DBException e) {
                log.warn("Can't extract constraint DDL", e);
            }
        }
        return super.getNestedDeclaration(monitor, owner, command, options);
    }

    @Override
    protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<PostgreTableConstraintBase> command) {
        if (command.getObject().getConstraintType() == DBSEntityConstraintType.CHECK) {
            decl.append(" (").append(((PostgreTableConstraint) command.getObject()).getSource()).append(")");
        } else {
            super.appendConstraintDefinition(decl, command);
        }
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            addConstraintCommentAction(actionList, command.getObject());
        }
    }

    static void addConstraintCommentAction(List<DBEPersistAction> actionList, PostgreTableConstraintBase constr) {
        actionList.add(new SQLDatabasePersistAction(
            "Comment sequence",
            "COMMENT ON CONSTRAINT " + DBUtils.getQuotedIdentifier(constr) + " ON " + constr.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                " IS " + SQLUtils.quoteString(constr, constr.getDescription())));
    }

    @Override
    protected String getDropConstraintPattern(PostgreTableConstraintBase constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
