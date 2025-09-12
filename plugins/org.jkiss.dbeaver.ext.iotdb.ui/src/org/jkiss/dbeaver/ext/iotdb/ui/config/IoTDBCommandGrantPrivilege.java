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

package org.jkiss.dbeaver.ext.iotdb.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBPrivilege;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBRelationalUser;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

public class IoTDBCommandGrantPrivilege extends DBECommandAbstract<IoTDBRelationalUser> {

    private int operationType;
    private String database;
    private String table;
    private IoTDBPrivilege privilege;

    public IoTDBCommandGrantPrivilege(IoTDBRelationalUser user,
                                      int type,
                                      String database,
                                      String table,
                                      IoTDBPrivilege privilege) {
        super(user, "Grant or Revoke Privilege");
        this.operationType = type;
        this.database = database;
        this.table = table;
        this.privilege = privilege;
    }

    /**
     * revoke grant [0]
     * grant [1]
     * grant with option [2]
     * revoke [3]
     *
     * @return
     */
    @NotNull
    @Override
    public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor,
                                                @NotNull DBCExecutionContext executionContext,
                                                @NotNull Map<String, Object> options) {
        String privilegeName = privilege.getName().toUpperCase();

        String grantScript = "";
        String revokeScript = "";
        if (database.isEmpty() || table.isEmpty()) {
            grantScript = "GRANT " + privilegeName + " TO USER " + getObject().getName();
            revokeScript = "REVOKE " + privilegeName + " FROM USER " + getObject().getName();
        } else {
            int scopeType = 0;
            if (database.equals("(ALL)")) {
                scopeType = 2;
            } else if (table.equals("(ALL)")) {
                scopeType = 1;
            }

            String scope = scopeType == 0 ? (database + "." + table) : (scopeType == 1 ? ("DATABASE " + database) : "ANY");

            grantScript = "GRANT " + privilegeName + " ON " + scope + " TO USER " + getObject().getName();
            revokeScript = "REVOKE " + privilegeName + " ON " + scope + " FROM USER " + getObject().getName();
        }

        String operationScript = switch (operationType) {
            case 0 -> revokeScript;
            case 1 -> grantScript;
            case 2 -> grantScript + " WITH GRANT OPTION";
            case 3 -> revokeScript;
            default -> "";
        };

        return operationType == 0 ? new DBEPersistAction[] {
            new SQLDatabasePersistAction("Grant or Revoke Privilege", operationScript),
            new SQLDatabasePersistAction("Grant or Revoke Privilege", grantScript)
        } : new DBEPersistAction[] {
            new SQLDatabasePersistAction("Grant or Revoke Privilege", operationScript)
        };
    }
}
