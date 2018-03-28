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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Postgre table manager
 */
public abstract class PostgreTableManagerBase extends SQLTableManager<PostgreTableBase, PostgreSchema> {

    protected void addObjectExtraActions(List<DBEPersistAction> actions, NestedObjectCommand<PostgreTableBase, PropertyHandler> command, Map<String, Object> options) {
        boolean isDDL = CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_SOURCE);
        PostgreTableBase table = command.getObject();
        // Add comments
        if ((!table.isPersisted() || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) && table.getDescription() != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON " + (table.isView() ? "VIEW": "TABLE") + " " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(table, table.getDescription())));
        }
        DBRProgressMonitor monitor = new VoidProgressMonitor();
        if (isDDL) {
            try {
                {
                    // Column comments
                    boolean hasComments = false;
                    for (PostgreTableColumn column : table.getAttributes(monitor)) {
                        if (!CommonUtils.isEmpty(column.getDescription())) {
                            if (!hasComments) {
                                actions.add(new SQLDatabasePersistActionComment(table.getDataSource(), "Column comments"));
                            }
                            PostgreTableColumnManager.addColumnCommentAction(actions, column);
                            hasComments = true;
                        }
                    }
                }

                {
                    // Constraint comments
                    boolean hasComments = false;
                    for (PostgreTableConstraintBase constr : table.getConstraints(monitor)) {
                        if (!CommonUtils.isEmpty(constr.getDescription())) {
                            if (!hasComments) {
                                actions.add(new SQLDatabasePersistActionComment(table.getDataSource(), "Constraint comments"));
                            }
                            PostgreConstraintManager.addConstraintCommentAction(actions, constr);
                            hasComments = true;
                        }
                    }
                    for (DBSEntityAssociation fk : table.getAssociations(monitor)) {
                        if (fk instanceof PostgreTableForeignKey && !CommonUtils.isEmpty(fk.getDescription())) {
                            if (!hasComments) {
                                actions.add(new SQLDatabasePersistActionComment(table.getDataSource(), "Foreign key comments"));
                            }
                            PostgreConstraintManager.addConstraintCommentAction(actions, (PostgreTableForeignKey)fk);
                            hasComments = true;
                        }
                    }
                }

                // Triggers
                if (table instanceof PostgreTableReal) {
                    Collection<PostgreTrigger> triggers = ((PostgreTableReal) table).getTriggers(monitor);
                    if (!CommonUtils.isEmpty(triggers)) {
                        actions.add(new SQLDatabasePersistActionComment(table.getDataSource(), "Table Triggers"));

                        for (PostgreTrigger trigger : triggers) {
                            actions.add(new SQLDatabasePersistAction("Create trigger", trigger.getObjectDefinitionText(monitor, options)));
                        }
                    }
                }

                getTableGrantPermissionActions(monitor, table, actions, options);
            } catch (DBException e) {
                log.error(e);
            }
        }
    }

    public static void getTableGrantPermissionActions(DBRProgressMonitor monitor, PostgreTableBase table, List<DBEPersistAction> actions, Map<String, Object> options) throws DBException {
        if (CommonUtils.getOption(options, PostgreConstants.OPTION_DDL_SHOW_PERMISSIONS)) {
            // Permissions
            Collection<PostgrePermission> permissions = table.getPermissions(monitor);
            if (!CommonUtils.isEmpty(permissions)) {
                actions.add(new SQLDatabasePersistActionComment(table.getDataSource(), "Permissions"));
                for (PostgrePermission permission : permissions) {
                    if (permission.hasAllPrivileges(table)) {
                        Collections.addAll(actions,
                            new PostgreCommandGrantPrivilege(permission.getOwner(), true, permission, PostgrePrivilegeType.ALL)
                                .getPersistActions(options));
                    } else {
                        PostgreCommandGrantPrivilege grant = new PostgreCommandGrantPrivilege(permission.getOwner(), true, permission, permission.getPrivileges());
                        Collections.addAll(actions, grant.getPersistActions(options));
                    }
                }
            }
        }
    }
}
