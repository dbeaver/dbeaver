/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2017 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableKeyColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableUniqueKey;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

import java.util.ArrayList;
import java.util.List;

/**
 * DB2 Unique Keys Manager
 * 
 * @author Denis Forveille
 */
public class DB2UniqueKeyManager extends SQLConstraintManager<DB2TableUniqueKey, DB2Table> {

    private static final String                    SQL_DROP_PK = "ALTER TABLE %s DROP PRIMARY KEY ";
    private static final String                    SQL_DROP_UK = "ALTER TABLE %s DROP UNIQUE %s";

    private static final DBSEntityConstraintType[] CONS_TYPES  = { DBSEntityConstraintType.PRIMARY_KEY,
        DBSEntityConstraintType.UNIQUE_KEY };

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public boolean canEditObject(DB2TableUniqueKey object)
    {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DB2TableUniqueKey> getObjectsCache(DB2TableUniqueKey object)
    {
        return object.getParentObject().getSchema().getConstraintCache();
    }

    // ------
    // Create
    // ------

    @Override
    public DB2TableUniqueKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final DB2Table table,
        Object from)
    {
        return new UITask<DB2TableUniqueKey>() {
            @Override
            protected DB2TableUniqueKey runTask()
            {
                EditConstraintPage editPage = new EditConstraintPage(DB2Messages.edit_db2_constraint_manager_dialog_title, table,
                    CONS_TYPES);

                if (!editPage.edit()) {
                    return null;
                }

                DB2TableUniqueKey constraint = new DB2TableUniqueKey(table, editPage.getConstraintType());
                constraint.setName(editPage.getConstraintName());

                List<DB2TableKeyColumn> columns = new ArrayList<>(editPage.getSelectedAttributes().size());
                DB2TableKeyColumn column;
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    column = new DB2TableKeyColumn(constraint, (DB2TableColumn) tableColumn, colIndex++);
                    columns.add(column);
                }
                constraint.setColumns(columns);

                return constraint;
            }
        }.execute();
    }

    // ------
    // DROP
    // ------

    @Override
    public String getDropConstraintPattern(DB2TableUniqueKey constraint)
    {
        String tablename = constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL);
        if (constraint.getConstraintType().equals(DBSEntityConstraintType.PRIMARY_KEY)) {
            return String.format(SQL_DROP_PK, tablename);
        } else {
            return String.format(SQL_DROP_UK, tablename, constraint.getName());
        }
    }

    @NotNull
    protected String getAddConstraintTypeClause(DB2TableUniqueKey constraint)
    {
        if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
            return "UNIQUE"; //$NON-NLS-1$
        }
        return super.getAddConstraintTypeClause(constraint);
    }
}
