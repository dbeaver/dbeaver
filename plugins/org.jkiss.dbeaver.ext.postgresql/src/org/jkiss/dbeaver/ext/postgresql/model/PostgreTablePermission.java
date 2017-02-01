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

