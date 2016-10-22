/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.*;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2DeleteUpdateRule;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DB2 Foreign key Manager
 * 
 * @author Denis Forveille
 */
public class DB2ForeignKeyManager extends SQLForeignKeyManager<DB2TableForeignKey, DB2Table> {

    private static final String SQL_DROP_FK = "ALTER TABLE %s DROP FOREIGN KEY %s";
    private static final String SQL_ALTER = "ALTER TABLE %s ALTER FOREIGN KEY %s";

    private static final String CONS_FK_NAME = "%s_%s_FK";

    private static final DBSForeignKeyModifyRule[] FK_RULES;

    static {
        List<DBSForeignKeyModifyRule> rules = new ArrayList<>(DB2DeleteUpdateRule.values().length);
        for (DB2DeleteUpdateRule db2DeleteUpdateRule : DB2DeleteUpdateRule.values()) {
            rules.add(db2DeleteUpdateRule.getRule());
        }
        FK_RULES = rules.toArray(new DBSForeignKeyModifyRule[] {});
    }

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
    public DB2TableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final DB2Table table, Object from)
    {
        return new UITask<DB2TableForeignKey>() {
            @Override
            protected DB2TableForeignKey runTask() {
                EditForeignKeyPage editDialog = new EditForeignKeyPage(
                    DB2Messages.edit_db2_foreign_key_manager_dialog_title, table, FK_RULES);
                if (!editDialog.edit()) {
                    return null;
                }

                DBSForeignKeyModifyRule deleteRule = editDialog.getOnDeleteRule();
                DBSForeignKeyModifyRule updateRule = editDialog.getOnUpdateRule();
                DB2TableUniqueKey ukConstraint = (DB2TableUniqueKey) editDialog.getUniqueConstraint();

                String tableName = CommonUtils.escapeIdentifier(table.getName());
                String targetTableName = CommonUtils.escapeIdentifier(editDialog.getUniqueConstraint().getParentObject().getName());

                DB2TableForeignKey foreignKey = new DB2TableForeignKey(table, ukConstraint, deleteRule, updateRule);

                String fkBaseName = String.format(CONS_FK_NAME, tableName, targetTableName);
                String fkName = DBObjectNameCaseTransformer.transformObjectName(foreignKey, fkBaseName);

                foreignKey.setName(fkName);

                List<DB2TableKeyColumn> columns = new ArrayList<>(editDialog.getColumns().size());
                DB2TableKeyColumn column;
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editDialog.getColumns()) {
                    column = new DB2TableKeyColumn(foreignKey, (DB2TableColumn) tableColumn.getOwnColumn(), colIndex++);
                    columns.add(column);
                }

                foreignKey.setColumns(columns);

                return foreignKey;
            }
        }.execute();
    }

    // ------
    // Alter
    // ------

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
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
