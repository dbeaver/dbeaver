/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
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

    public static final String OPTION_REF_TABLE = "refTable";
    public static final String OPTION_REF_CONSTRAINT = "refConstraint";
    public static final String OPTION_REF_ATTRIBUTES = "refAttributes";
    public static final String OPTION_OWN_ATTRIBUTES = "ownAttributes";

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_create_new_foreign_key,
                "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD " + getNestedDeclarationScript(table, command, options)) //$NON-NLS-1$ //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
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
    protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, TABLE_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command, Map<String, Object> options) {
        return getNestedDeclarationScript(owner, command, options);
    }

    private StringBuilder getNestedDeclarationScript(TABLE_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command, Map<String, Object> options)
    {
        OBJECT_TYPE foreignKey = command.getObject();
        boolean legacySyntax = isLegacyForeignKeySyntax(owner);
        boolean constraintDuplicated = isFKConstraintDuplicated(owner);

        // Create column
        String constraintName = DBUtils.getQuotedIdentifier(foreignKey.getDataSource(), foreignKey.getName());

        StringBuilder decl = new StringBuilder(40);
        if (!legacySyntax || !foreignKey.isPersisted() || constraintDuplicated) {
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
            refConstraint == null ? "<?>" : DBUtils.getEntityScriptName(refConstraint.getParentObject(), options);

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
        appendUpdateDeleteRule(foreignKey, decl);

        if (legacySyntax) {
            decl.append(" CONSTRAINT ").append(constraintName); //$NON-NLS-1$
        }
        return decl;
    }

    protected void appendUpdateDeleteRule(OBJECT_TYPE foreignKey, StringBuilder decl) {
        DBSForeignKeyModifyRule deleteRule = foreignKey.getDeleteRule();
        if (deleteRule != null && !CommonUtils.isEmpty(deleteRule.getClause())) {
            decl.append(" ON DELETE ").append(deleteRule.getClause()); //$NON-NLS-1$
        }
        DBSForeignKeyModifyRule updateRule = foreignKey.getUpdateRule();
        if (updateRule != null && !CommonUtils.isEmpty(updateRule.getClause())) {
            decl.append(" ON UPDATE ").append(updateRule.getClause()); //$NON-NLS-1$
        }
    }

    protected String getDropForeignKeyPattern(OBJECT_TYPE constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected String getNewConstraintName(DBRProgressMonitor monitor, OBJECT_TYPE foreignKey) {

        DBSEntityConstraint uniqueKey = foreignKey.getReferencedConstraint();
        DBSEntity targetTable = uniqueKey == null ? null : uniqueKey.getParentObject();

        TABLE_TYPE table = foreignKey.getParentObject();
        String baseName = CommonUtils.escapeIdentifier(table.getName()) + "_" + //$NON-NLS-1$
            (uniqueKey == null ? "" : CommonUtils.escapeIdentifier(targetTable.getName()) + "_") + "FK"; //$NON-NLS-1$

        DBSObjectCache<? extends DBSObject, OBJECT_TYPE> objectsCache = getObjectsCache(foreignKey);
        if (objectsCache == null) {
            return baseName;
        }
        for (int i = 0; ; i++) {
            String constrName = DBObjectNameCaseTransformer.transformName(foreignKey.getDataSource(), i == 0 ? baseName : (baseName + "_" + i));
            DBSObject child = objectsCache.getCachedObject(constrName);
            if (child == null) {
                return constrName;
            }
        }
    }

    protected <T extends DBSEntityConstraint> T getReferencedKey(DBRProgressMonitor monitor, TABLE_TYPE table, Class<T> refKeyClass, Map<String, Object> options) {
        Object refConstraint = options.get(OPTION_REF_CONSTRAINT);
        if (refKeyClass.isInstance(refConstraint)) {
            return refKeyClass.cast(refConstraint);
        }
        Object refTable = options.get(OPTION_REF_TABLE);
        if (refTable instanceof DBSEntity) {
            Object refAttrs = options.get(OPTION_REF_ATTRIBUTES);
            if (refAttrs instanceof Collection) {
                try {
                    DBSEntityConstraint entityConstraint = DBUtils.findEntityConstraint(
                        monitor,
                        (DBSEntity) refTable,
                        (Collection<? extends DBSEntityAttribute>) refAttrs);
                    if (refKeyClass.isInstance(entityConstraint)) {
                        return refKeyClass.cast(entityConstraint);
                    }
                } catch (DBException e) {
                    log.debug("Error searchign constraint by attributes", e);
                }
            }
        }
        return null;
    }

    protected boolean isLegacyForeignKeySyntax(TABLE_TYPE owner) {
        return false;
    }

    protected boolean isFKConstraintDuplicated(TABLE_TYPE owner) {
        return false;
    }
}

