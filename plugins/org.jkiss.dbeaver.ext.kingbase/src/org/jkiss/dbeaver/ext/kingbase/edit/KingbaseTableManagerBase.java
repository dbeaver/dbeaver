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
package org.jkiss.dbeaver.ext.kingbase.edit;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseIndex;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSchema;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTable;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableBase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableColumn;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableConstraintBase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableForeignKey;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableReal;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTrigger;
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

/**
 * Kingbase table manager
 */
public abstract class KingbaseTableManagerBase extends SQLTableManager<KingbaseTableBase, KingbaseSchema> {

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<KingbaseTableBase, PropertyHandler> command, @NotNull Map<String, Object> options) {
        boolean isDDL = CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_SOURCE);
        KingbaseTableBase table = command.getObject();
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
            KingbaseDataSource dataSource = table.getDataSource();
            boolean addExtraActionComment =
                table.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_EXTRA_DDL_INFO);
            try {
                if (showComments) {
                    // Column comments
                    boolean hasComments = false;
                    for (KingbaseTableColumn column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
                        if (!CommonUtils.isEmpty(column.getDescription())) {
                            if (!hasComments && addExtraActionComment) {
                                actions.add(new SQLDatabasePersistActionComment(dataSource, "Column comments"));
                            }
                            KingbaseTableColumnManager.addColumnCommentAction(actions, column);
                            hasComments = true;
                        }
                    }
                }

                if (showComments && !table.isPartition() && !monitor.isCanceled()) {
                    // Constraint comments
                    boolean hasComments = false;
                    for (KingbaseTableConstraintBase constr : CommonUtils.safeCollection(table.getConstraints(monitor))) {
                        if (!CommonUtils.isEmpty(constr.getDescription())) {
                            if (!hasComments && addExtraActionComment) {
                                actions.add(new SQLDatabasePersistActionComment(dataSource, "Constraint comments"));
                            }
                            KingbaseConstraintManager.addConstraintCommentAction(actions, constr);
                            hasComments = true;
                        }
                    }
                    for (DBSEntityAssociation fk : CommonUtils.safeCollection(table.getAssociations(monitor))) {
                        if (fk instanceof KingbaseTableForeignKey && !CommonUtils.isEmpty(fk.getDescription())) {
                            if (!hasComments && addExtraActionComment) {
                                actions.add(new SQLDatabasePersistActionComment(dataSource, "Foreign key comments"));
                            }
                            KingbaseConstraintManager.addConstraintCommentAction(actions, (KingbaseTableForeignKey)fk);
                            hasComments = true;
                        }
                    }
                }

                // Triggers
                if (table instanceof KingbaseTableReal && !table.isPartition() && !monitor.isCanceled()) {
                    Collection<KingbaseTrigger> triggers = ((KingbaseTableReal) table).getTriggers(monitor);
                    if (!CommonUtils.isEmpty(triggers)) {
                        if (addExtraActionComment) {
                            actions.add(new SQLDatabasePersistActionComment(dataSource, "Table Triggers"));
                        }

                        for (KingbaseTrigger trigger : triggers) {
                            actions.add(new SQLDatabasePersistAction("Create trigger", trigger.getObjectDefinitionText(monitor, options)));
                        }
                    }
                }

                // Partitions
                if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_PARTITIONS)
                    && table instanceof KingbaseTable
                    && !monitor.isCanceled()
                ) {
                    KingbaseTable kingbaseTable = (KingbaseTable) table;
                    List<KingbaseTableBase> partitions = kingbaseTable.getPartitions(monitor);
                    if (kingbaseTable.hasPartitions() && !CommonUtils.isEmpty(partitions)) {
                        if (addExtraActionComment) {
                            actions.add(new SQLDatabasePersistActionComment(dataSource, "Partitions"));
                        }
                        for (KingbaseTableBase partition : partitions) {
                            actions.add(
                                new SQLDatabasePersistAction("Create partition", partition.getObjectDefinitionText(monitor, options)));
                        }
                    }
                }

                if (isDDL && !table.isPartition() && !monitor.isCanceled()) {
                    KingbaseUtils.getObjectGrantPermissionActions(monitor, table, actions, options);
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
    }

    @Override
    protected boolean isIncludeIndexInDDL(DBRProgressMonitor monitor, DBSTableIndex index) throws DBException {
        return !((KingbaseIndex)index).isPrimaryKeyIndex() && super.isIncludeIndexInDDL(monitor, index);
    }

    @Override
    protected boolean isIncludeDropInDDL(@NotNull KingbaseTableBase table) {
        return !table.isPartition();
    }
}
