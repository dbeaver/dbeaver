/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Mimer SQL shadow - an online replica of a databank used for backup / failover.
 * Read from {@code INFORMATION_SCHEMA.EXT_SHADOWS}.
 *
 * @author Mimer Information Technology
 */
public class MimerShadow implements DBSObject {

    private final MimerDataSource dataSource;
    private final String name;
    private final String creator;
    private final String databankName;
    private final String fileName;
    private final boolean online;

    public MimerShadow(@NotNull MimerDataSource dataSource, @NotNull JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(dbResult, "SHADOW_NAME");
        this.creator = JDBCUtils.safeGetString(dbResult, "SHADOW_CREATOR");
        this.databankName = JDBCUtils.safeGetString(dbResult, "DATABANK_NAME");
        this.fileName = JDBCUtils.safeGetString(dbResult, "FILE_NAME");
        this.online = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_ONLINE"));
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public String getDatabankName() {
        return databankName;
    }

    @Property(viewable = true, order = 3)
    public String getFileName() {
        return fileName;
    }

    @Property(viewable = true, order = 4)
    public String getCreator() {
        return creator;
    }

    @Property(viewable = true, order = 5)
    public boolean isOnline() {
        return online;
    }

    @Nullable
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
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return dataSource;
    }
}
