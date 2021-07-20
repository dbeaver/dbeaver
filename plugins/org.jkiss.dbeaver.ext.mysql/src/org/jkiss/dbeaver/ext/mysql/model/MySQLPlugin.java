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
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

public class MySQLPlugin extends MySQLInformation implements DBPStatefulObject {

    private final String name;
    private final Status status;
    private final String type;
    private final String library;
    private final String license;

    public MySQLPlugin(@NotNull MySQLDataSource dataSource, @NotNull ResultSet dbResult) {
        super(dataSource);
        this.name = JDBCUtils.safeGetString(dbResult, "NAME");
        this.status = CommonUtils.valueOf(Status.class, JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"), Status.UNKNOWN);
        this.type = JDBCUtils.safeGetString(dbResult, "TYPE");
        this.library = JDBCUtils.safeGetString(dbResult, "LIBRARY");
        this.license = JDBCUtils.safeGetString(dbResult, "LICENSE");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public Status getStatus() {
        return status;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public String getType() {
        return type;
    }

    @Nullable
    @Property(viewable = true, order = 4)
    public String getLibrary() {
        return library;
    }

    @NotNull
    @Property(viewable = true, order = 4)
    public String getLicense() {
        return license;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        if (status == Status.ACTIVE) {
            return DBSObjectState.ACTIVE;
        }
        if (status == Status.UNKNOWN) {
            return DBSObjectState.UNKNOWN;
        }
        return DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) {
        // do nothing
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        DISABLED,
        DELETING,
        DELETED,
        UNKNOWN
    }
}
