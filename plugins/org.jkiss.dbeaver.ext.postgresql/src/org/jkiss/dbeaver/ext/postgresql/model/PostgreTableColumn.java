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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * PostgreTableColumn
 */
public class PostgreTableColumn extends PostgreAttribute<PostgreTableBase> implements PostgrePermissionsOwner
{
    public PostgreTableColumn(PostgreTableBase table) {
        super(table);
    }

    public PostgreTableColumn(PostgreTableBase table, JDBCResultSet dbResult) throws DBException {
        super(table, dbResult);
    }

    @Override
    public PostgreSchema getSchema() {
        return getTable().getSchema();
    }

    @Override
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return getTable().getOwner(monitor);
    }

    @Override
    public Collection<PostgrePermission> getPermissions(DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        return PostgreUtils.extractPermissionsFromACL(this, getAcl());
    }

}
