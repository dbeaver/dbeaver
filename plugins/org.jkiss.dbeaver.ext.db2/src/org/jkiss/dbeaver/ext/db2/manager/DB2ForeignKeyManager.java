/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

import java.util.List;
import java.util.Map;

/**
 * DB2 Foreign key Manager
 * 
 * @author Denis Forveille
 */
public class DB2ForeignKeyManager extends SQLForeignKeyManager<DB2TableForeignKey, DB2Table> {

    private static final String SQL_DROP_FK = "ALTER TABLE %s DROP FOREIGN KEY %s";
    private static final String SQL_ALTER = "ALTER TABLE %s ALTER FOREIGN KEY %s";

    private static final String CONS_FK_NAME = "%s_%s_FK";

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public boolean canEditObject(DB2TableForeignKey object)
    {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DB2TableForeignKey> getObjectsCache(DB2TableForeignKey object)
    {
        return object.getParentObject().getSchema().getAssociationCache();
    }

    // ------
    // Create
    // ------
    @Override
    public DB2TableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object table, Object from, Map<String, Object> options)
    {
        return new DB2TableForeignKey(
            (DB2Table) table,
            null,
            DBSForeignKeyModifyRule.NO_ACTION,
            DBSForeignKeyModifyRule.NO_ACTION);
    }

    // ------
    // Alter
    // ------

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        // DF: Throw exception for now
        // Will have to implement it for alter FK query optimisation + TRUST
        throw new IllegalStateException("Object modification is not supported in " + getClass().getSimpleName()); //$NON-NLS-1$
    }

    // ------
    // Drop
    // ------
    @Override
    public String getDropForeignKeyPattern(DB2TableForeignKey foreignKey)
    {
        String tableName = foreignKey.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL);
        return String.format(SQL_DROP_FK, tableName, foreignKey.getName());
    }

}
