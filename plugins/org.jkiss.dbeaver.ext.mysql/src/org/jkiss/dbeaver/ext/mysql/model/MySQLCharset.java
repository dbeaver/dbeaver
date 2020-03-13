/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MySQLCharset
 */
public class MySQLCharset extends MySQLInformation {

    private String name;
    private String description;
    private int maxLength;
    private List<MySQLCollation> collations = new ArrayList<>();

    public MySQLCharset(MySQLDataSource dataSource, ResultSet dbResult)
        throws SQLException
    {
        super(dataSource);
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CHARSET);
        this.description = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DESCRIPTION);
        this.maxLength = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_MAX_LEN);
    }

    void addCollation(MySQLCollation collation)
    {
        collations.add(collation);
        Collections.sort(collations, DBUtils.nameComparator());
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public List<MySQLCollation> getCollations()
    {
        return collations;
    }

    @Property(viewable = true, order = 2)
    public MySQLCollation getDefaultCollation()
    {
        for (MySQLCollation collation : collations) {
            if (collation.isDefault()) {
                return collation;
            }
        }
        return null;
    }

    public MySQLCollation getCollation(String name) {
        for (MySQLCollation collation : collations) {
            if (collation.getName().equals(name)) {
                return collation;
            }
        }
        return null;
    }

    @Property(viewable = true, order = 3)
    public int getMaxLength()
    {
        return maxLength;
    }

    @Nullable
    @Override
    @Property(viewable = true, multiline = true, order = 100)
    public String getDescription()
    {
        return description;
    }

}
