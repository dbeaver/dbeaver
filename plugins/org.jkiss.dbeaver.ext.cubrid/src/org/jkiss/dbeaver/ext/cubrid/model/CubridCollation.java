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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CubridCollation implements DBSObject
{
    private String name;
    private CubridCharset charset;
    private CubridDataSource dataSource;

    protected CubridCollation(@NotNull String name) {
        this.name = name;
    }

    protected CubridCollation(@NotNull CubridCharset charset, @NotNull ResultSet dbResult) throws SQLException {
        this.name = JDBCUtils.safeGetString(dbResult, CubridConstants.COLLATION);
        this.charset = charset;
        this.dataSource = charset.getDataSource();
    }

    @NotNull
    public CubridDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public CubridCharset getCharset() {
        return charset;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }
}
