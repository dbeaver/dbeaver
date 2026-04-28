/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.ai.AISchemaGenerationOptions;
import org.jkiss.dbeaver.model.ai.AISchemaGenerator;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AISchemaGeneratorImpl implements AISchemaGenerator {
    private static final Log log = Log.getLog(AISchemaGeneratorImpl.class);

    @Override
    public String generateSchema(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBCExecutionContext executionContext,
        @NotNull AISchemaGenerationOptions options,
        @NotNull DBSEntity entity
    ) throws DBException {
        StringBuilder ddl = new StringBuilder();

        if (options.sendFullDDL()) {
            String tableDDL = DBStructUtils.generateTableDDL(
                monitor,
                entity,
                Map.of(DBPScriptObject.OPTION_SKIP_DROPS, true),
                false
            );
            DBStructUtils.addDDLLine(ddl, tableDDL);
        } else {
            generateCustomDDL(monitor, executionContext, options, entity, ddl);
            ddl.append(SQLConstants.DEFAULT_STATEMENT_DELIMITER).append(GeneralUtils.getDefaultLineSeparator());
        }

        if (options.sendReferences() && executionContext != null) {
            if (entity instanceof DBSTable table) {
                // Always generate native DDL because it uses FQNs and ALTER TABLE syntax
                addReferencesDDL(monitor, executionContext, table, true, ddl);
            }
        }

        return ddl.toString().trim();
    }

    private void addReferencesDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSTable table,
        boolean nativeDDL,
        @NotNull StringBuilder ddl
    ) {
        try {
            Collection<? extends DBSEntityAssociation> references = table.getReferences(monitor);
            if (!CommonUtils.isEmpty(references)) {
                final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();
                for (DBSEntityAssociation refKey : references) {
                    if (refKey instanceof DBSTableForeignKey tfk) {
                        String fkScript;
                        if (nativeDDL) {
                            final SQLObjectEditor<?, ?> entityEditor = editorsRegistry.getObjectManager(
                                refKey.getClass(),
                                SQLObjectEditor.class
                            );
                            if (entityEditor instanceof SQLForeignKeyManager fkm) {
                                DBEPersistAction[] ddlActions = fkm.makeCreateCommand(tfk, Map.of())
                                    .getPersistActions(monitor, executionContext, Map.of());
                                fkScript = SQLUtils.generateScript(
                                    table.getDataSource(),
                                    ddlActions,
                                    false
                                );
                            } else {
                                fkScript = null;
                            }
                        } else {
                            fkScript = describeForeignKey(monitor, refKey);
                        }
                        if (!CommonUtils.isEmpty(fkScript)) {
                            DBStructUtils.addDDLLine(ddl, fkScript);
                        }
                    }
                }
            }
        } catch (DBException e) {
            log.debug("Error reading table '" + table.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                "' references", e);
        }
    }

    @NotNull
    private String generateCustomDDL(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBCExecutionContext executionContext,
        @NotNull AISchemaGenerationOptions options,
        @NotNull DBSEntity table,
        @NotNull StringBuilder ddl
    ) throws DBException {
        String name = options.useFQN() || requiresFqn(table, executionContext)
            ? DBUtils.getObjectFullName(table.getDataSource(), table, DBPEvaluationContext.DDL)
            : DBUtils.getQuotedIdentifier(table);

        if (options.sendObjectComment()) {
            String tableDescription = describe(table);
            if (!tableDescription.isBlank()) {
                ddl.append(tableDescription).append("\n");
            }
        }

        if (DBUtils.isView(table)) {
            ddl.append("CREATE VIEW ");
        } else {
            ddl.append("CREATE TABLE ");
        }

        ddl.append(name);

        List<? extends DBSEntityAttribute> attributes = table.getAttributes(monitor);

        if (attributes == null || attributes.isEmpty()) {
            return ddl.append(");").toString();
        }

        StringJoiner joiner = new StringJoiner(",", " (", ") ");
        attributes.forEach(attr -> {
            if (DBUtils.isHiddenObject(attr)) {
                return;
            }

            joiner.add(
                DBUtils.getQuotedIdentifier(attr)
                    + (options.sendColumnTypes() ? " " + attr.getTypeName() : "")
                    + (options.sendObjectComment() && !describe(attr).isBlank() ? describe(attr) : "")
            );
        });

        if (options.sendConstraints()) {
            describeTableKeys(monitor, table).forEach(joiner::add);
        }

        if (options.sendForeignKeys()) {
            describeForeignKeys(monitor, table).forEach(joiner::add);
        }

        ddl.append(joiner);
        return ddl.toString();
    }

    private static boolean requiresFqn(
        @NotNull DBSObject obj,
        @Nullable DBCExecutionContext ctx
    ) {
        if (ctx == null || ctx.getContextDefaults() == null) {
            return false;
        }
        DBSObject parent = obj.getParentObject();
        DBCExecutionContextDefaults<?, ?> def = ctx.getContextDefaults();
        return parent != null
            && !(parent.equals(def.getDefaultCatalog()) || parent.equals(def.getDefaultSchema()));
    }

    private List<String> describeTableKeys(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity entity
    ) throws DBException {
        Collection<? extends DBSEntityConstraint> constraints = entity.getConstraints(monitor);
        if (CommonUtils.isEmpty(constraints)) {
            return List.of();
        }

        List<String> keyDescriptions = new ArrayList<>(constraints.size());
        for (DBSEntityConstraint c : constraints) {
            String description = describeConstraint(monitor, c);
            if (!CommonUtils.isEmpty(description)) {
                keyDescriptions.add(description);
            }
        }

        return keyDescriptions;
    }

    @Nullable
    private String describeConstraint(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityConstraint constraint
    ) throws DBException {
        DBSEntityConstraintType type = constraint.getConstraintType();
        if (type != DBSEntityConstraintType.PRIMARY_KEY
            && type != DBSEntityConstraintType.UNIQUE_KEY) {
            return null;
        }

        List<? extends DBSEntityAttributeRef> refs = ((DBSEntityReferrer) constraint).getAttributeReferences(monitor);
        if (CommonUtils.isEmpty(refs)) {
            return null;
        }

        String attrs = refs.stream()
            .map(DBSEntityAttributeRef::getAttribute)
            .filter(Objects::nonNull)
            .map(DBPNamedObject::getName)
            .collect(Collectors.joining(", ", "(", ")"));

        return (type == DBSEntityConstraintType.PRIMARY_KEY ? "PRIMARY KEY " : "UNIQUE ") + attrs;
    }


    @NotNull
    private List<String> describeForeignKeys(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity object
    ) throws DBException {
        Collection<? extends DBSEntityAssociation> associations = object.getAssociations(monitor);
        if (associations == null || associations.isEmpty()) {
            return List.of();
        }

        List<String> fkDdls = new ArrayList<>(associations.size());
        for (DBSEntityAssociation association : associations) {
            String fkDescription = describeForeignKey(monitor, association);
            if (!CommonUtils.isEmpty(fkDescription)) {
                fkDdls.add(fkDescription);
            }
        }

        return fkDdls;
    }

    @Nullable
    private String describeForeignKey(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAssociation association
    ) throws DBException {
        if (!(association instanceof DBSEntityReferrer referrer)) {
            return null;
        }

        StringBuilder description = new StringBuilder();

        DBSEntity refEntity = association.getAssociatedEntity();
        List<? extends DBSEntityAttributeRef> refAttrs = referrer.getAttributeReferences(monitor);
        if (refEntity == null || CommonUtils.isEmpty(refAttrs)) {
            return null;
        }

        String attrs = refAttrs.stream()
            .map(DBSEntityAttributeRef::getAttribute)
            .filter(Objects::nonNull)
            .map(DBPNamedObject::getName)
            .collect(Collectors.joining(", ", "(", ")"));

        description.append("FOREIGN KEY ")
            .append(attrs)
            .append(" REFERENCES ").append(refEntity.getName());

        return description.toString();
    }

    @NotNull
    private static String describe(@NotNull DBPObjectWithDescription object) {
        String description = object.getDescription();
        if (description == null || description.isBlank()) {
            return "";
        } else {
            return "-- " + description + "\n";
        }
    }
}
