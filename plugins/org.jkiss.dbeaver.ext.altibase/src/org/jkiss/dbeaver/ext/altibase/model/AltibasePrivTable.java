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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

/**
 * AltibasePrivTable
 */
public class AltibasePrivTable extends AltibaseObject<AltibaseTable> implements DBAPrivilege {
    private String grantee;
    private String grantor;
    private String granteeType;
    private boolean grantable;

    public AltibasePrivTable(AltibaseTable table, ResultSet resultSet) {
        super(table, JDBCUtils.safeGetString(resultSet, "PRIV_NAME"), true);
        this.grantee = JDBCUtils.safeGetString(resultSet, "GRANTEE_NAME");
        this.grantor = JDBCUtils.safeGetString(resultSet, "GRANTOR_NAME");
        this.granteeType = JDBCUtils.safeGetString(resultSet, "GRANTEE_TYPE");
        this.grantable = JDBCUtils.safeGetBoolean(resultSet, "WITH_GRANT_OPTION", AltibaseConstants.RESULT_1_VALUE);
    }
    
    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName() {
        return super.getName();
    }
    
    @Property(viewable = true, order = 10)
    public Object getGrantor(DBRProgressMonitor monitor) throws DBException {
        if (monitor == null) {
            return grantor;
        }
        return getDataSource().getGrantee(monitor, grantor);
    }
    
    @Property(viewable = true, order = 11)
    public Object getGrantee(DBRProgressMonitor monitor) throws DBException {
        if (monitor == null) {
            return grantee;
        }
        return getDataSource().getGrantee(monitor, grantee);
    }
    
    @Property(viewable = true, order = 12)
    public String getGranteeType() throws DBException {
        return granteeType;
    }
    
    @Property(viewable = true, order = 13)
    public boolean isGrantable() {
        return grantable;
    }
}
