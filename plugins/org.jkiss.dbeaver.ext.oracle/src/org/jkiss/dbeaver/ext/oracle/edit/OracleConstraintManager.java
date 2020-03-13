/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectStatus;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableConstraint;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * Oracle constraint manager
 */
public class OracleConstraintManager extends SQLConstraintManager<OracleTableConstraint, OracleTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTableConstraint> getObjectsCache(OracleTableConstraint object)
    {
        return object.getParentObject().getSchema().constraintCache;
    }

    @Override
    protected OracleTableConstraint createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        OracleTableBase table = (OracleTableBase) container;

        return new OracleTableConstraint(
            table,
            "",
            DBSEntityConstraintType.UNIQUE_KEY,
            null,
            OracleObjectStatus.ENABLED);
    }

    @Override
    protected String getDropConstraintPattern(OracleTableConstraint constraint)
    {
        String clause = "CONSTRAINT"; //$NON-NLS-1$;
/*
        if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
            clause = "PRIMARY KEY"; //$NON-NLS-1$
        } else {
            clause = "CONSTRAINT"; //$NON-NLS-1$
        }
*/
        return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP " + clause + " " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command, Map<String, Object> options)
    {
    	OracleTableConstraint constraint = (OracleTableConstraint) command.getObject();
    	OracleTableBase table = constraint.getTable();
        actions.add(
                new SQLDatabasePersistAction(
                    ModelMessages.model_jdbc_create_new_constraint,
                    "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD " + getNestedDeclaration(monitor, table, command, options) +
                    " "  + (constraint.getStatus() == OracleObjectStatus.ENABLED ? "ENABLE" : "DISABLE" )
                	));
    }

}
