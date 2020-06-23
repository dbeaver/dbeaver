/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableCheckConstraint;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

public class MySQLCheckConstraintManager  extends SQLConstraintManager<MySQLTableCheckConstraint, MySQLTable> {
    @Override
    protected MySQLTableCheckConstraint createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        return new MySQLTableCheckConstraint(
                (MySQLTable) container,
                "NewConstraint",
                null,
                DBSEntityConstraintType.CHECK,
                false);
    }

    @Override
    public DBSObjectCache<MySQLCatalog, MySQLTableCheckConstraint> getObjectsCache(MySQLTableCheckConstraint object) {
        return object.getTable().getContainer().getCheckConstraintCache();
    }

    @Override
    protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<MySQLTableCheckConstraint> command) {
        if (command.getObject().getConstraintType() == DBSEntityConstraintType.CHECK) {
            decl.append(" (").append((command.getObject()).getClause()).append(")");
        } else {
            super.appendConstraintDefinition(decl, command);
        }
    }
}
