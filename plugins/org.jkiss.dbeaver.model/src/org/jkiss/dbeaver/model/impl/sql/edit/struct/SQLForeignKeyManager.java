/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * JDBC foreign key manager
 */
public abstract class SQLForeignKeyManager<OBJECT_TYPE extends JDBCTableConstraint<TABLE_TYPE> & DBSTableForeignKey, TABLE_TYPE extends JDBCTable>
    extends SQLObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_create_new_foreign_key,
                "ALTER TABLE " + table.getFullQualifiedName() + " ADD " + getNestedDeclaration(table, command)) //$NON-NLS-1$ //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_foreign_key,
                getDropForeignKeyPattern(command.getObject())
                    .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullQualifiedName())
                    .replace(PATTERN_ITEM_CONSTRAINT, command.getObject().getName()))
        );
    }

    @Override
    protected StringBuilder getNestedDeclaration(TABLE_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command)
    {
        OBJECT_TYPE foreignKey = command.getObject();
        boolean legacySyntax = isLegacyForeignKeySyntax(owner);

        // Create column
        String constraintName = DBUtils.getQuotedIdentifier(foreignKey.getDataSource(), foreignKey.getName());

        StringBuilder decl = new StringBuilder(40);
        if (!legacySyntax || !foreignKey.isPersisted()) {
            decl.append("CONSTRAINT ");
        }
        if (!legacySyntax) {
            decl.append(constraintName).append(" "); //$NON-NLS-1$
        }
        decl.append(foreignKey.getConstraintType().getName().toUpperCase(Locale.ENGLISH)) //$NON-NLS-1$
            .append(" ("); //$NON-NLS-1$
        try {
            // Get columns using void monitor
            final Collection<? extends DBSEntityAttributeRef> columns = command.getObject().getAttributeReferences(VoidProgressMonitor.INSTANCE);
            boolean firstColumn = true;
            for (DBSEntityAttributeRef constraintColumn : CommonUtils.safeCollection(columns)) {
                final DBSEntityAttribute attribute = constraintColumn.getAttribute();
                if (!firstColumn) decl.append(","); //$NON-NLS-1$
                firstColumn = false;
                if (attribute != null) {
                    decl.append(attribute.getName());
                }
            }
        } catch (DBException e) {
            log.error("Can't obtain reference attributes", e);
        }
        final DBSEntityConstraint refConstraint = foreignKey.getReferencedConstraint();
        decl.append(") REFERENCES ").append(refConstraint == null ? "<?>" : DBUtils.getObjectFullName(refConstraint.getParentObject())).append("("); //$NON-NLS-1$ //$NON-NLS-2$
        if (refConstraint instanceof DBSEntityReferrer) {
            try {
                boolean firstColumn = true;
                List<? extends DBSEntityAttributeRef> columns = ((DBSEntityReferrer) refConstraint).getAttributeReferences(VoidProgressMonitor.INSTANCE);
                for (DBSEntityAttributeRef constraintColumn : CommonUtils.safeCollection(columns)) {
                    if (!firstColumn) decl.append(","); //$NON-NLS-1$
                    firstColumn = false;
                    final DBSEntityAttribute attribute = constraintColumn.getAttribute();
                    if (attribute != null) {
                        decl.append(DBUtils.getQuotedIdentifier(attribute));
                    }
                }
            } catch (DBException e) {
                log.error("Can't obtain ref constraint reference attributes", e);
            }
        }
        decl.append(")"); //$NON-NLS-1$
        if (foreignKey.getDeleteRule() != null && !CommonUtils.isEmpty(foreignKey.getDeleteRule().getClause())) {
            decl.append(" ON DELETE ").append(foreignKey.getDeleteRule().getClause()); //$NON-NLS-1$
        }
        if (foreignKey.getUpdateRule() != null && !CommonUtils.isEmpty(foreignKey.getUpdateRule().getClause())) {
            decl.append(" ON UPDATE ").append(foreignKey.getUpdateRule().getClause()); //$NON-NLS-1$
        }

        if (legacySyntax) {
            decl.append(" CONSTRAINT ").append(constraintName); //$NON-NLS-1$
        }
        return decl;
    }

    protected String getDropForeignKeyPattern(OBJECT_TYPE constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected boolean isLegacyForeignKeySyntax(TABLE_TYPE owner) {
        return false;
    }
}

