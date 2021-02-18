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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MySQLCollation
 */
public class MySQLCollation extends MySQLInformation {

    private MySQLCharset charset;
    private int id;
    private String name;
    private boolean isDefault;
    private boolean isCompiled;
    private int sortLength;

    public MySQLCollation(MySQLCharset charset, ResultSet dbResult)
        throws SQLException
    {
        super(charset.getDataSource());
        this.charset = charset;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLLATION);
        this.id = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_ID);
        this.isDefault = "Yes".equalsIgnoreCase(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT));
        this.isCompiled = "Yes".equalsIgnoreCase(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COMPILED));
        this.sortLength = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_SORT_LENGTH);
    }

    @Property(viewable = true, order = 2)
    public MySQLCharset getCharset()
    {
        return charset;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 3)
    public int getId()
    {
        return id;
    }

    @Property(viewable = true, order = 4)
    public boolean isDefault()
    {
        return isDefault;
    }

    @Property(viewable = true, order = 5)
    public boolean isCompiled()
    {
        return isCompiled;
    }

    @Property(viewable = true, order = 6)
    public int getSortLength()
    {
        return sortLength;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return charset;
    }
}
