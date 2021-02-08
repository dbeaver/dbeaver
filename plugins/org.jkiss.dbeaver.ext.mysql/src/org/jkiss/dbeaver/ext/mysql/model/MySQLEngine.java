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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MySQLEngine
 */
public class MySQLEngine extends MySQLInformation {

    public enum Support {
        YES,
        NO,
        DEFAULT,
        DISABLED
    }

    public static final String MYISAM = "MyISAM";

    private String name;
    private String description;
    private Support support;
    private boolean supportsTransactions;
    private boolean supportsXA;
    private boolean supportsSavepoints;

    public MySQLEngine(MySQLDataSource dataSource, ResultSet dbResult)
        throws SQLException
    {
        super(dataSource);
        this.loadInfo(dbResult);
    }

    public MySQLEngine(MySQLDataSource dataSource, String name) {
        super(dataSource);
        this.name = name;
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_NAME);
        this.description = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_DESCRIPTION);
        this.support = Support.valueOf(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT));
        this.supportsTransactions = "YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT_TXN));
        this.supportsXA = "YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT_XA));
        this.supportsSavepoints = "YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT_SAVEPOINTS));
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

//    @Property(name = "Description", viewable = true, order = 100)
    @Nullable
    @Override
    public String getDescription()
    {
        return description;
    }

    @Property(viewable = true, order = 3)
    public Support getSupport() {
        return support;
    }

    @Property(viewable = true, order = 4)
    public boolean isSupportsTransactions()
    {
        return supportsTransactions;
    }

    @Property(viewable = true, order = 5)
    public boolean isSupportsXA()
    {
        return supportsXA;
    }

    @Property(viewable = true, order = 6)
    public boolean isSupportsSavepoints()
    {
        return supportsSavepoints;
    }

}
