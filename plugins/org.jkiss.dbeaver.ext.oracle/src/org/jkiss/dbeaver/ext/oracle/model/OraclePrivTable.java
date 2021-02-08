/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

/**
 * OraclePrivTable
 */
public class OraclePrivTable extends OracleObject<OracleTableBase> implements DBAPrivilege {
    private String grantee;
    private String grantor;
    private boolean grantable;
    private boolean hierarchy;

    public OraclePrivTable(OracleTableBase table, ResultSet resultSet) {
        super(table, JDBCUtils.safeGetString(resultSet, "PRIVILEGE"), true);
        this.grantee = JDBCUtils.safeGetString(resultSet, "GRANTEE");
        this.grantor = JDBCUtils.safeGetString(resultSet, "GRANTOR");
        this.grantable = JDBCUtils.safeGetBoolean(resultSet, "GRANTABLE", "Y");
        this.hierarchy = JDBCUtils.safeGetBoolean(resultSet, "HIERARCHY", "Y");
    }

    @Property(viewable = true, order = 1)
    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }

    @Property(viewable = true, order = 5, supportsPreview = true)
    public Object getGrantee(DBRProgressMonitor monitor) throws DBException
    {
        if (monitor == null) {
            return grantee;
        }
        return getDataSource().getGrantee(monitor, grantee);
    }

    @Property(viewable = true, order = 6, supportsPreview = true)
    public Object getGrantor(DBRProgressMonitor monitor) throws DBException
    {
        if (monitor == null) {
            return grantor;
        }
        return getDataSource().getGrantee(monitor, grantor);
    }

    @Property(viewable = true, order = 10)
    public boolean isGrantable()
    {
        return grantable;
    }

    @Property(viewable = true, order = 11)
    public boolean isHierarchy()
    {
        return hierarchy;
    }
}
