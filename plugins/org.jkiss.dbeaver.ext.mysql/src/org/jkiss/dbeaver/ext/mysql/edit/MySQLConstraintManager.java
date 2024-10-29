/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableConstraint;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

/**
 * MySQL constraint manager
 */
public class MySQLConstraintManager extends SQLConstraintManager<MySQLTableConstraint, MySQLTable> {

    @Nullable
    @Override
    public DBSObjectCache<MySQLCatalog, MySQLTableConstraint> getObjectsCache(MySQLTableConstraint object)
    {
        if (object.getConstraintType() == DBSEntityConstraintType.CHECK) {
            return object.getTable().getContainer().getCheckConstraintCache();
        } else {
            return object.getTable().getContainer().getUniqueKeyCache();
        }
    }

    @Override
    protected MySQLTableConstraint createDatabaseObject(
        @NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container,
        Object from, @NotNull Map<String, Object> options)
    {
        return new MySQLTableConstraint(
            (MySQLTable) container,
            "NewConstraint",
            null,
            DBSEntityConstraintType.PRIMARY_KEY,
            false);
    }

    @NotNull
    @Override
    protected String getAddConstraintTypeClause(MySQLTableConstraint constraint) {
        if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
            return MySQLConstants.CONSTRAINT_UNIQUE; //$NON-NLS-1$
        } else if (constraint.getConstraintType() == DBSEntityConstraintType.CHECK) {
            return MySQLConstants.CONSTRAINT_CHECK; //$NON-NLS-1$
        }
        return super.getAddConstraintTypeClause(constraint);
    }

    @Override
    protected String getDropConstraintPattern(MySQLTableConstraint constraint)
    {
        if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
            return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP PRIMARY KEY"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } else if (constraint.getConstraintType() == DBSEntityConstraintType.CHECK) {
            return "ALTER TABLE " + constraint.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " DROP CONSTRAINT " + DBUtils.getQuotedIdentifier(constraint);
        } else {
            return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP KEY " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    @Override
    protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<MySQLTableConstraint> command) {
        if (command.getObject().getConstraintType() == DBSEntityConstraintType.CHECK) {
            decl.append(" (").append((command.getObject()).getCheckClause()).append(")");
        } else {
            super.appendConstraintDefinition(decl, command);
        }
    }
}
