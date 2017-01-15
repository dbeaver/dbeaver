/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * PostgreTablePermission
 */
public class PostgreTablePermission extends PostgrePermission {

    private static final Log log = Log.getLog(PostgreTablePermission.class);

    private String grantee;

    public PostgreTablePermission(PostgrePermissionsOwner owner, String grantee, List<PostgrePrivilege> privileges) {
        super(owner, privileges);
        this.grantee = grantee;
    }

    @Property(viewable = true, order = 1)
    @NotNull
    public String getName() {
        return grantee;
    }

    @Override
    public PostgreRole getTargetObject(DBRProgressMonitor monitor) throws DBException
    {
        return owner.getDatabase().roleCache.getObject(monitor, owner.getDatabase(), grantee);
    }

    public String getGrantee() {
        return grantee;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(@NotNull PostgrePermission o) {
        if (o instanceof PostgreTablePermission) {
            return grantee.compareTo(((PostgreTablePermission)o).grantee);
        }
        return 0;
    }

}

