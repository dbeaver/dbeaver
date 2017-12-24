/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JDBC constraint manager
 */
public abstract class SQLConstraintManager<OBJECT_TYPE extends JDBCTableConstraint<TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends SQLObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        final TABLE_TYPE table = command.getObject().getTable();

        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_create_new_constraint,
                "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD " + getNestedDeclaration(table, command, options)));
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
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
    public StringBuilder getNestedDeclaration(TABLE_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command, Map<String, Object> options)
    {
        OBJECT_TYPE constraint = command.getObject();

        // Create column
        String constraintName = DBUtils.getQuotedIdentifier(constraint.getDataSource(), constraint.getName());

        boolean legacySyntax = isLegacyConstraintsSyntax(owner);
        StringBuilder decl = new StringBuilder(40);
        if (!legacySyntax || !constraint.isPersisted()) {
            decl.append("CONSTRAINT "); //$NON-NLS-1$
        }
        if (!legacySyntax) {
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
                }
            }
        } catch (DBException e) {
            log.warn("Can't obtain attribute references", e);
        }
        decl.append(")"); //$NON-NLS-1$
    }

    @NotNull
    protected String getAddConstraintTypeClause(OBJECT_TYPE constraint) {
        return constraint.getConstraintType().getName().toUpperCase(Locale.ENGLISH);
    }

    protected String getDropConstraintPattern(OBJECT_TYPE constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected boolean isLegacyConstraintsSyntax(TABLE_TYPE owner) {
        return false;
    }
}

