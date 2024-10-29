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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Postgre table manager
 */
public abstract class PostgreTableManagerBase extends SQLTableManager<PostgreTableBase, PostgreSchema> {

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<PostgreTableBase, PropertyHandler> command, @NotNull Map<String, Object> options) {
        boolean isDDL = CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_SOURCE);
        PostgreTableBase table = command.getObject();
        // Add comments
        String comment;
        if (!table.isPersisted()) {
            Object descProp = command.getProperty(DBConstants.PROP_ID_DESCRIPTION);
            comment = descProp != null ? descProp.toString() : null;
        } else {
            comment = table.getDescription();
        }
        boolean showComments =
            CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_COMMENTS) ||
            CommonUtils.getOption(options, DBPScriptObject.OPTION_OBJECT_SAVE);
        if ((showComments && !CommonUtils.isEmpty(comment)) || command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON " + table.getTableTypeName() + " " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(table, CommonUtils.notEmpty(comment))));
        }
        if (isDDL || !table.isPersisted()) {
            // show comment commands for DDL and new objects
            PostgreDataSource dataSource = table.getDataSource();
            boolean addExtraActionComment =
                table.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_EXTRA_DDL_INFO);
            try {
                if (showComments) {
                    // Column comments
                    boolean hasComments = false;
                    for (PostgreTableColumn column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
                        if (!CommonUtils.isEmpty(column.getDescription())) {
                            if (!hasComments && addExtraActionComment) {
                                actions.add(new SQLDatabasePersistActionComment(dataSource, "Column comments"));
                            }
                            PostgreTableColumnManager.addColumnCommentAction(actions, column);
                            hasComments = true;
                        }
                    }
                }

                if (showComments && !table.isPartition() && !monitor.isCanceled()) {
                    // Constraint comments
                    boolean hasComments = false;
                    for (PostgreTableConstraintBase constr : CommonUtils.safeCollection(table.getConstraints(monitor))) {
                        if (!CommonUtils.isEmpty(constr.getDescription())) {
                            if (!hasComments && addExtraActionComment) {
                                actions.add(new SQLDatabasePersistActionComment(dataSource, "Constraint comments"));
                            }
                            PostgreConstraintManager.addConstraintCommentAction(actions, constr);
                            hasComments = true;
                        }
                    }
                    for (DBSEntityAssociation fk : CommonUtils.safeCollection(table.getAssociations(monitor))) {
                        if (fk instanceof PostgreTableForeignKey && !CommonUtils.isEmpty(fk.getDescription())) {
                            if (!hasComments && addExtraActionComment) {
                                actions.add(new SQLDatabasePersistActionComment(dataSource, "Foreign key comments"));
                            }
                            PostgreConstraintManager.addConstraintCommentAction(actions, (PostgreTableForeignKey)fk);
                            hasComments = true;
                        }
                    }
                }

                // Triggers
                if (table instanceof PostgreTableReal && !table.isPartition() && !monitor.isCanceled()) {
                    Collection<PostgreTrigger> triggers = ((PostgreTableReal) table).getTriggers(monitor);
                    if (!CommonUtils.isEmpty(triggers)) {
                        if (addExtraActionComment) {
                            actions.add(new SQLDatabasePersistActionComment(dataSource, "Table Triggers"));
                        }

                        for (PostgreTrigger trigger : triggers) {
                            actions.add(new SQLDatabasePersistAction("Create trigger", trigger.getObjectDefinitionText(monitor, options)));
                        }
                    }
                }

                // Rules
                if (table instanceof PostgreTableReal && !table.isPartition() && !monitor.isCanceled()) {
                    Collection<PostgreRule> rules = ((PostgreTableReal) table).getRules(monitor);
                    if (!CommonUtils.isEmpty(rules)) {
                        if (addExtraActionComment) {
                            actions.add(new SQLDatabasePersistActionComment(dataSource, "Table Rules"));
                        }

                        for (PostgreRule rule : rules) {
                            actions.add(new SQLDatabasePersistAction("Create rule", rule.getObjectDefinitionText(monitor, options)));
                        }
                    }
                }

                // Partitions
                if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_PARTITIONS)
                    && table instanceof PostgreTable
                    && !monitor.isCanceled()
                ) {
                    PostgreTable postgreTable = (PostgreTable) table;
                    List<PostgreTableBase> partitions = postgreTable.getPartitions(monitor);
                    if (postgreTable.hasPartitions() && !CommonUtils.isEmpty(partitions)) {
                        if (addExtraActionComment) {
                            actions.add(new SQLDatabasePersistActionComment(dataSource, "Partitions"));
                        }
                        for (PostgreTableBase partition : partitions) {
                            actions.add(
                                new SQLDatabasePersistAction("Create partition", partition.getObjectDefinitionText(monitor, options)));
                        }
                    }
                }

                if (isDDL && !table.isPartition() && !monitor.isCanceled()) {
                    PostgreUtils.getObjectGrantPermissionActions(monitor, table, actions, options);
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
    }

    @Override
    protected boolean isIncludeIndexInDDL(DBRProgressMonitor monitor, DBSTableIndex index) throws DBException {
        return !((PostgreIndex)index).isPrimaryKeyIndex() && super.isIncludeIndexInDDL(monitor, index);
    }

    @Override
    protected boolean isIncludeDropInDDL(@NotNull PostgreTableBase table) {
        return !table.isPartition();
    }
}
