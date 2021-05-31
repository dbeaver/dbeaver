/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLEngine;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableConstraint;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableForeignKey;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * MySQL foreign key manager
 */
public class MySQLForeignKeyManager extends SQLForeignKeyManager<MySQLTableForeignKey, MySQLTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MySQLTableForeignKey> getObjectsCache(MySQLTableForeignKey object)
    {
        return object.getParentObject().getForeignKeyCache();
    }

    @Override
    protected MySQLTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
    {
        MySQLTable table = (MySQLTable) container;
        try {
            if (MySQLEngine.MYISAM.equalsIgnoreCase(table.getAdditionalInfo(monitor).getEngine().getName())) {
                DBWorkbench.getPlatformUI().showError("Create foreign key", "Foreign keys are not supported by MyISAM engine.\n" +
                    "You could change table's engine to INNODB or some other relational engine");
                return null;
            }
        } catch (DBCException e) {
            log.error(e);
            return null;
        }
        MySQLTableForeignKey foreignKey = new MySQLTableForeignKey(
            table,
            "",
            null,
            getReferencedKey(monitor, table, MySQLTableConstraint.class, options),
            DBSForeignKeyModifyRule.NO_ACTION,
            DBSForeignKeyModifyRule.NO_ACTION,
            false);
        foreignKey.setName(getNewConstraintName(monitor, foreignKey));
        return foreignKey;
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        addObjectDeleteActions(monitor, executionContext, actions, new ObjectDeleteCommand(command.getObject(), command.getTitle()), options);
        addObjectCreateActions(monitor, executionContext, actions, makeCreateCommand(command.getObject(), options), options);
    }

    @Override
    protected String getDropForeignKeyPattern(MySQLTableForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP FOREIGN KEY " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
