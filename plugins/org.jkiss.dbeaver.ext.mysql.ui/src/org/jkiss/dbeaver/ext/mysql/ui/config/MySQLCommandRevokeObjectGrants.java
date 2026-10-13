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
package org.jkiss.dbeaver.ext.mysql.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Revokes a set of privileges of one grant (as listed in SHOW GRANTS) from a user.
 * Used by the "-" buttons of the privileges editor to strip all rights on an object at once.
 */
public class MySQLCommandRevokeObjectGrants extends DBECommandAbstract<MySQLUser> {

    private final String objectSpec;
    private final List<String> privilegeNames;
    // Column-level privileges of the same grant: a separate statement, mixing levels is rejected by the server
    private final List<String> columnPrivilegeNames;
    private final boolean grantOption;

    /**
     * Creates a command that revokes the given privileges on a single object from a user.
     *
     * @param objectSpec     fully rendered grant target, e.g. {@code `db`.*}, {@code `db`.`tbl`}
     *                       or {@code PROCEDURE `db`.`proc`}
     * @param privilegeNames rendered privilege names, possibly with column lists,
     *                       e.g. {@code SELECT}, {@code UPDATE (`col1`, `col2`)}, {@code ALL PRIVILEGES}
     */
    public MySQLCommandRevokeObjectGrants(
        @NotNull MySQLUser user,
        @NotNull String objectSpec,
        @NotNull List<String> privilegeNames,
        boolean grantOption) {
        this(user, objectSpec, privilegeNames, List.of(), grantOption);
    }

    /**
     * Creates a command that removes a whole grant row in one undoable step: table-level and
     * column-level privileges are revoked by two statements of this single command.
     *
     * @param privilegeNames       rendered table/routine-level privileges, e.g. {@code SELECT}, {@code ALL PRIVILEGES}
     * @param columnPrivilegeNames rendered column-level privileges, e.g. {@code UPDATE (`col1`, `col2`)}
     */
    public MySQLCommandRevokeObjectGrants(
        @NotNull MySQLUser user,
        @NotNull String objectSpec,
        @NotNull List<String> privilegeNames,
        @NotNull List<String> columnPrivilegeNames,
        boolean grantOption
    ) {
        super(user, MySQLUIMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.objectSpec = objectSpec;
        this.privilegeNames = new ArrayList<>(privilegeNames);
        this.columnPrivilegeNames = new ArrayList<>(columnPrivilegeNames);
        this.grantOption = grantOption;
    }

    @Override
    public void updateModel() {
        getObject().clearGrantsCache();
    }

    @Nullable
    @Override
    public DBEPersistAction[] getPersistActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull Map<String, Object> options) {
        List<DBEPersistAction> actions = new ArrayList<>();
        List<String> revoked = new ArrayList<>(privilegeNames);
        List<String> columnRevoked = new ArrayList<>(columnPrivilegeNames);
        // Fold grant option into one REVOKE statement with other privileges (the table-level one
        // when there is one); it is revoked on its own only when the grant holds nothing else.
        if (grantOption) {
            if (revoked.isEmpty() && !columnRevoked.isEmpty()) {
                columnRevoked.add("GRANT OPTION"); //$NON-NLS-1$
            } else {
                revoked.add("GRANT OPTION"); //$NON-NLS-1$
            }
        }
        addRevokeAction(actions, revoked);
        addRevokeAction(actions, columnRevoked);
        return actions.toArray(new DBEPersistAction[0]);
    }

    private void addRevokeAction(@NotNull List<DBEPersistAction> actions, @NotNull List<String> revoked) {
        if (!revoked.isEmpty()) {
            actions.add(new SQLDatabasePersistAction(
                MySQLUIMessages.edit_command_grant_privilege_name_revoke_privilege,
                "REVOKE " + String.join(", ", revoked) + //$NON-NLS-1$ //$NON-NLS-2$
                    " ON " + objectSpec + //$NON-NLS-1$
                    " FROM " + getObject().getFullName())); //$NON-NLS-1$
        }
    }
}
