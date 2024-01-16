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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * @author Shengkai Bai
 */
public class DamengPrivObject implements DBAPrivilege {

    private DamengRole damengRole;

    private String object;

    private String objectType;

    private String privilege;

    private String grantor;


    public DamengPrivObject(DamengRole damengRole, ResultSet resultSet) {
        this.damengRole = damengRole;
        this.object = JDBCUtils.safeGetString(resultSet, "OBJECT_NAME");
        this.objectType = JDBCUtils.safeGetString(resultSet, "SUB_TYPE");
        this.privilege = JDBCUtils.safeGetString(resultSet, "PRIV");
        this.grantor = JDBCUtils.safeGetString(resultSet, "GRANTOR_NAME");
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return object;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return damengRole;
    }

    @Override
    public DBPDataSource getDataSource() {
        return damengRole.getDataSource();
    }

    @Property(viewable = true, order = 2)
    public String getObjectType() {
        return objectType;
    }

    @Property(viewable = true, order = 3)
    public String getPrivilege() {
        return privilege;
    }

    @Property(viewable = true, order = 4)
    public String getGrantor() {
        return grantor;
    }
}
