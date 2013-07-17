/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mssql.MSSQLMessages;
import org.jkiss.dbeaver.ext.mssql.model.MSSQLCatalog;
import org.jkiss.dbeaver.ext.mssql.model.MSSQLPrivilege;
import org.jkiss.dbeaver.ext.mssql.model.MSSQLTableBase;
import org.jkiss.dbeaver.ext.mssql.model.MSSQLUser;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class MSSQLCommandGrantPrivilege extends DBECommandAbstract<MSSQLUser> {

    private boolean grant;
    private MSSQLCatalog schema;
    private MSSQLTableBase table;
    private MSSQLPrivilege privilege;

    public MSSQLCommandGrantPrivilege(MSSQLUser user, boolean grant, MSSQLCatalog schema, MSSQLTableBase table, MSSQLPrivilege privilege)
    {
        super(user, grant ? MSSQLMessages.edit_command_grant_privilege_action_grant_privilege : MSSQLMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.grant = grant;
        this.schema = schema;
        this.table = table;
        this.privilege = privilege;
    }

    @Override
    public void updateModel()
    {
        getObject().clearGrantsCache();
    }

    @Override
    public IDatabasePersistAction[] getPersistActions()
    {
        String privName = privilege.getName();
        String grantScript = "GRANT " + privName + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " TO " + getObject().getFullName() + ""; //$NON-NLS-1$ //$NON-NLS-2$
        String revokeScript = "REVOKE " + privName + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " FROM " + getObject().getFullName() + ""; //$NON-NLS-1$ //$NON-NLS-2$
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                MSSQLMessages.edit_command_grant_privilege_action_grant_privilege,
                grant ? grantScript : revokeScript)
        };
    }

    @Override
    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams)
    {
        if (prevCommand instanceof MSSQLCommandGrantPrivilege) {
            MSSQLCommandGrantPrivilege prevGrant = (MSSQLCommandGrantPrivilege)prevCommand;
            if (prevGrant.schema == schema && prevGrant.table == table && prevGrant.privilege == privilege) {
                if (prevGrant.grant == grant) {
                    return prevCommand;
                } else {
                    return null;
                }
            }
        }
        return super.merge(prevCommand, userParams);
    }

    private String getObjectName()
    {
        return
            (schema == null ? "*" : DBUtils.getQuotedIdentifier(schema)) + "." + //$NON-NLS-1$ //$NON-NLS-2$
            (table == null ? "*" : DBUtils.getQuotedIdentifier(table)); //$NON-NLS-1$
    }

}
