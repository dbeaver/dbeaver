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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
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
import java.util.Map;

/**
 * JDBC foreign key manager
 */
public abstract class SQLForeignKeyManager<OBJECT_TYPE extends JDBCTableConstraint<TABLE_TYPE> & DBSTableForeignKey, TABLE_TYPE extends JDBCTable>
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
                ModelMessages.model_jdbc_create_new_foreign_key,
                "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD " + getNestedDeclaration(table, command, options)) //$NON-NLS-1$ //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_foreign_key,
                getDropForeignKeyPattern(command.getObject())
                    .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
                    .replace(PATTERN_ITEM_CONSTRAINT, DBUtils.getQuotedIdentifier(command.getObject())))
        );
    }

    @Override
    protected StringBuilder getNestedDeclaration(TABLE_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command, Map<String, Object> options)
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
            final Collection<? extends DBSEntityAttributeRef> columns = command.getObject().getAttributeReferences(new VoidProgressMonitor());
            boolean firstColumn = true;
            for (DBSEntityAttributeRef constraintColumn : CommonUtils.safeCollection(columns)) {
                final DBSEntityAttribute attribute = constraintColumn.getAttribute();
                if (!firstColumn) decl.append(","); //$NON-NLS-1$
                firstColumn = false;
                if (attribute != null) {
                    decl.append(DBUtils.getQuotedIdentifier(attribute));
                }
            }
        } catch (DBException e) {
            log.error("Can't obtain reference attributes", e);
        }
        final DBSEntityConstraint refConstraint = foreignKey.getReferencedConstraint();

        final String refTableName =
            refConstraint == null ? "<?>" :
                (CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true) ?
                DBUtils.getObjectFullName(refConstraint.getParentObject(), DBPEvaluationContext.DDL) : DBUtils.getQuotedIdentifier(refConstraint.getParentObject()));

        decl.append(") REFERENCES ").append(refTableName).append("("); //$NON-NLS-1$ //$NON-NLS-2$
        if (refConstraint instanceof DBSEntityReferrer) {
            try {
                boolean firstColumn = true;
                List<? extends DBSEntityAttributeRef> columns = ((DBSEntityReferrer) refConstraint).getAttributeReferences(new VoidProgressMonitor());
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

