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

import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class PostgreCommandGrantPrivilege extends DBECommandAbstract<PostgrePermissionsOwner> {

    private boolean grant;
    private PostgrePermission permission;
    private PostgrePrivilegeType[] privilege;

    public PostgreCommandGrantPrivilege(PostgrePermissionsOwner user, boolean grant, PostgrePermission permission, PostgrePrivilegeType ... privilege)
    {
        super(user, grant ? PostgreMessages.edit_command_grant_privilege_action_grant_privilege : PostgreMessages.edit_command_grant_privilege_action_revoke_privilege);
        this.grant = grant;
        this.permission = permission;
        this.privilege = privilege;
    }

    @Override
    public void updateModel()
    {
        //getObject().clearGrantsCache();
    }

    @Override
    public DBEPersistAction[] getPersistActions(Map<String, Object> options)
    {
        StringBuilder privName = new StringBuilder();
        if (privilege == null) {
            privName = new StringBuilder(PostgrePrivilegeType.ALL.name());
        } else {
            for (PostgrePrivilegeType pn : privilege) {
                if (privName.length() > 0) privName.append(", ");
                privName.append(pn.name());
            }
        }

        PostgrePermissionsOwner object = getObject();
        String objectName, roleName;
        if (object instanceof PostgreRole) {
            roleName = DBUtils.getQuotedIdentifier(object);
            objectName = ((PostgreRolePermission)permission).getFullObjectName();
        } else {
            roleName = DBUtils.getQuotedIdentifier(object.getDataSource(), ((PostgreObjectPermission) permission).getGrantee());
            if (object instanceof PostgreProcedure) {
                objectName = ((PostgreProcedure) object).getUniqueName();
            } else {
                objectName = DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
            }
        }

        String objectType;
        if (permission instanceof PostgreRolePermission) {
            objectType = ((PostgreRolePermission) permission).getKind().name();
        } else if (object instanceof PostgreSequence) {
            objectType = "SEQUENCE";
        } else if (object instanceof PostgreProcedure) {
            objectType = "FUNCTION";
        } else {
            objectType = "TABLE";
        }
        String grantScript = "GRANT " + privName + //$NON-NLS-1$
            " ON " + objectType + " " + objectName + //$NON-NLS-1$
            " TO " + roleName + ""; //$NON-NLS-1$ //$NON-NLS-2$
        String revokeScript = "REVOKE " + privName + //$NON-NLS-1$
            " ON " + objectType + " " + objectName + //$NON-NLS-1$
            " FROM " + roleName + ""; //$NON-NLS-1$ //$NON-NLS-2$
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                PostgreMessages.edit_command_grant_privilege_action_grant_privilege,
                grant ? grantScript : revokeScript)
        };
    }

    @Override
    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams)
    {
        if (prevCommand instanceof PostgreCommandGrantPrivilege) {
            PostgreCommandGrantPrivilege prevGrant = (PostgreCommandGrantPrivilege)prevCommand;
            if (prevGrant.permission == permission && prevGrant.privilege == privilege) {
                if (prevGrant.grant == grant) {
                    return prevCommand;
                } else {
                    return null;
                }
            }
        }
        return super.merge(prevCommand, userParams);
    }

}
