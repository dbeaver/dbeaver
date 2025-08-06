/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.struct.AbstractTable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexOrdering;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JDBC constraint manager
 */
public abstract class SQLConstraintManager<OBJECT_TYPE extends AbstractTableConstraint, TABLE_TYPE extends AbstractTable>
    extends SQLObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options)
    {
        final TABLE_TYPE table = (TABLE_TYPE) command.getObject().getTable();

        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_create_new_constraint,
                "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD " + getNestedDeclaration(monitor, table, command, options)));
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_constraint,
                getDropConstraintPattern(command.getObject())
                    .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
                    .replace(PATTERN_ITEM_CONSTRAINT, DBUtils.getQuotedIdentifier(command.getObject())))
        );
    }

    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, TABLE_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command, Map<String, Object> options)
    {
        OBJECT_TYPE constraint = command.getObject();

        // Create column
        String constraintName = DBUtils.getQuotedIdentifier(constraint.getDataSource(), constraint.getName());

        boolean legacySyntax = isLegacyConstraintsSyntax(owner);
        boolean shortNotation = isShortNotation(owner);
        StringBuilder decl = new StringBuilder(40);
        if ((!legacySyntax || !constraint.isPersisted()) && !shortNotation) {
            decl.append("CONSTRAINT "); //$NON-NLS-1$
        }
        if (!legacySyntax && !shortNotation) {
            decl.append(constraintName).append(" ");
        }
        decl.append(getAddConstraintTypeClause(constraint));

        appendConstraintDefinition(decl, command);

        if (legacySyntax) {
            decl.append(" CONSTRAINT ").append(constraintName); //$NON-NLS-1$
        }
        return decl;
    }

    protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<OBJECT_TYPE> command) {
        decl.append(" ("); //$NON-NLS-1$
        // Get columns using void monitor
        try {
            List<? extends DBSEntityAttributeRef> attrs = command.getObject().getAttributeReferences(new VoidProgressMonitor());
            if (attrs != null) {
                boolean firstColumn = true;
                for (DBSEntityAttributeRef constraintColumn : attrs) {
                    final DBSEntityAttribute attribute = constraintColumn.getAttribute();
                    if (attribute == null) {
                        continue;
                    }
                    if (!firstColumn) decl.append(","); //$NON-NLS-1$
                    firstColumn = false;
                    decl.append(DBUtils.getQuotedIdentifier(attribute));
                    if (isPrimaryKeyOrdered() && constraintColumn instanceof DBSTableIndexOrdering ordering && !ordering.isAscending()) {
                        decl.append(" DESC");
                    }
                }
            }
        } catch (DBException e) {
            log.warn("Can't obtain attribute references", e);
        }
        decl.append(")"); //$NON-NLS-1$
    }

    @NotNull
    protected String getAddConstraintTypeClause(OBJECT_TYPE constraint) {
        if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
            return "UNIQUE"; //$NON-NLS-1$
        }
        return constraint.getConstraintType().getName().toUpperCase(Locale.ENGLISH);
    }

    protected String getDropConstraintPattern(OBJECT_TYPE constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected boolean isLegacyConstraintsSyntax(TABLE_TYPE owner) {
        return false;
    }

    protected boolean isShortNotation(TABLE_TYPE owner) {
        return false;
    }

    protected boolean isPrimaryKeyOrdered() {
        return false;
    }
}

