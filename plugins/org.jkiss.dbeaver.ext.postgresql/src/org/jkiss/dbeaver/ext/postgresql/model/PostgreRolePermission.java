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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * PostgreRolePermission
 */
public class PostgreRolePermission extends PostgrePermission {

    private static final Log log = Log.getLog(PostgreRolePermission.class);

    private PostgrePrivilege.Kind kind;
    private String schemaName;
    private String objectName;

    public PostgreRolePermission(PostgrePermissionsOwner owner, PostgrePrivilege.Kind kind, String schemaName, String objectName, List<PostgrePrivilege> privileges) {
        super(owner, privileges);
        this.kind = kind;
        this.schemaName = schemaName;
        this.objectName = objectName;
    }

    @Property(viewable = true, order = 1)
    @NotNull
    public String getName() {
        return getFullObjectName();
    }

    @Override
    public PostgreObject getTargetObject(DBRProgressMonitor monitor) throws DBException
    {
        final PostgreSchema schema = owner.getDatabase().getSchema(monitor, schemaName);
        if (schema != null) {
            JDBCTable childTable = schema.getChild(monitor, objectName);
            return childTable instanceof PostgreObject ? (PostgreObject) childTable : null;
        }
        return null;
    }

    public PostgrePrivilege.Kind getKind() {
        return kind;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getFullObjectName() {
        return DBUtils.getQuotedIdentifier(getDataSource(), schemaName) + "." +
            (kind == PostgrePrivilege.Kind.FUNCTION ? objectName : DBUtils.getQuotedIdentifier(getDataSource(), objectName));
    }

    @Override
    public String toString() {
        return getFullObjectName();
    }

    @Override
    public int compareTo(@NotNull PostgrePermission o) {
        if (o instanceof PostgreRolePermission) {
            final int res = schemaName.compareTo(((PostgreRolePermission)o).schemaName);
            return res != 0 ? res : objectName.compareTo(((PostgreRolePermission)o).objectName);
        }
        return 0;
    }

}

