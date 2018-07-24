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
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class PostgreCommandGrantPrivilege extends DBECommandAbstract<PostgrePermissionsOwner> {

    private boolean grant;
    private PostgrePermission permission;
    private PostgrePrivilegeType[] privilege;

    public PostgreCommandGrantPrivilege(PostgrePermissionsOwner user, boolean grant, PostgrePermission permission, PostgrePrivilegeType[] privilege)
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
    public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, Map<String, Object> options)
    {
        boolean withGrantOption = false;
        StringBuilder privName = new StringBuilder();
        if (privilege == null) {
            privName = new StringBuilder(PostgrePrivilegeType.ALL.name());
        } else {
            for (PostgrePrivilegeType pn : privilege) {
                if (privName.length() > 0) privName.append(", ");
                privName.append(pn.name());
                if ((permission.getPermission(pn) & PostgrePermission.WITH_GRANT_OPTION) != 0) {
                    withGrantOption = true;
                }

            }
        }

        PostgrePermissionsOwner object = getObject();
        String objectName, roleName;
        if (object instanceof PostgreRole) {
            roleName = DBUtils.getQuotedIdentifier(object);
            objectName = ((PostgreRolePermission)permission).getFullObjectName();
        } else {
            PostgreObjectPermission permission = (PostgreObjectPermission) this.permission;
            roleName = permission.getGrantee() == null ? null : DBUtils.getQuotedIdentifier(object.getDataSource(), permission.getGrantee());
            objectName = PostgreUtils.getObjectUniqueName(object);
        }
        if (roleName == null) {
            return new DBEPersistAction[0];
        }

        String objectType;
        if (permission instanceof PostgreRolePermission) {
            objectType = ((PostgreRolePermission) permission).getKind().name();
        } else {
            objectType = PostgreUtils.getObjectTypeName(object);
        }
        String grantScript =
            grant ?
                (object instanceof PostgreTableColumn ?
                    "GRANT " + privName + "(" + DBUtils.getQuotedIdentifier(object) + ") ON " + ((PostgreTableColumn) object).getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " TO " + roleName :
                    "GRANT " + privName + " ON " + objectType + " " + objectName + " TO " + roleName) :
                (object instanceof PostgreTableColumn ?
                    "REVOKE " + privName + "(" + DBUtils.getQuotedIdentifier(object) + ") ON " + ((PostgreTableColumn) object).getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " FROM " + roleName :
                    "REVOKE " + privName + " ON " + objectType + " " + objectName + " FROM " + roleName);
        if (grant && withGrantOption) {
            grantScript += " WITH GRANT OPTION";
        }
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                PostgreMessages.edit_command_grant_privilege_action_grant_privilege,
                grantScript)
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
