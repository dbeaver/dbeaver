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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Postgre table manager
 */
public class PostgreDDLUtils {

    protected static final Log log = Log.getLog(PostgreDDLUtils.class);

    static void addObjectExtraActions(List<DBEPersistAction> actions, PostgreTableBase table, Map<String, Object> options) {
        // Add comments
        if (!CommonUtils.isEmpty(table.getDescription())) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON " + (table.isView() ? "VIEW": "TABLE") + " " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(table, table.getDescription())));
        }
        DBRProgressMonitor monitor = new VoidProgressMonitor();
        try {
            {
                // Column comments
                boolean hasComments = false;
                for (PostgreTableColumn column : table.getAttributes(monitor)) {
                    if (!CommonUtils.isEmpty(column.getDescription())) {
                        if (!hasComments) {
                            actions.add(new SQLDatabasePersistActionComment(table.getDataSource(), "Column comments"));
                        }
                        actions.add(new SQLDatabasePersistAction("Set column comment", "COMMENT ON COLUMN " +
                            DBUtils.getObjectFullName(table, DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
                            " IS " + SQLUtils.quoteString(column, column.getDescription())));
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
        } catch (DBException e) {
            log.error(e);
        }
    }

}
