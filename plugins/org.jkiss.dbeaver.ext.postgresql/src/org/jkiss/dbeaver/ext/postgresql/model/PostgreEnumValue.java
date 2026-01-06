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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

public class PostgreEnumValue implements PostgreObject {

    private PostgreDataSource dataSource;
    private PostgreDatabase database;

    private long oid;
    private long enumTypId;
    private long enumSortOrder;
    private String enumLabel;

    public PostgreEnumValue(@NotNull PostgreDataSource dataSource, @NotNull PostgreDatabase database, @NotNull JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.database = database;
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.enumTypId = JDBCUtils.safeGetLong(dbResult, "enumtypid");
        this.enumSortOrder = JDBCUtils.safeGetLong(dbResult, "enumsortorder");
        this.enumLabel = JDBCUtils.safeGetString(dbResult, "enumlabel");
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return database;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }

    @NotNull
    @Override
    public String getName() {
        return CommonUtils.toString(enumTypId);
    }

    public long getOid() {
        return oid;
    }

    public long getEnumTypId() {
        return enumTypId;
    }

    public long getEnumSortOrder() {
        return enumSortOrder;
    }

    public String getEnumLabel() {
        return enumLabel;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public long getObjectId() {
        return oid;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }
}
