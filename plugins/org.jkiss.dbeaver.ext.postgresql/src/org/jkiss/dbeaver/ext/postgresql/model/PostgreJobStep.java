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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Map;

public class PostgreJobStep implements PostgreObject, PostgreScriptObject, DBPStatefulObject {
    private final PostgreJob job;
    private final long id;
    private final String name;
    private final String description;
    private final String code;
    private final StepKind kind;
    private final boolean enabled;

    public PostgreJobStep(@NotNull PostgreJob job, @NotNull ResultSet dbResult) {
        this.job = job;
        this.id = JDBCUtils.safeGetInt(dbResult, "jstid");
        this.name = JDBCUtils.safeGetString(dbResult, "jstname");
        this.description = JDBCUtils.safeGetString(dbResult, "jstdesc");
        this.code = JDBCUtils.safeGetString(dbResult, "jstcode");
        this.kind = CommonUtils.valueOf(StepKind.class, JDBCUtils.safeGetString(dbResult, "jstkind"));
        this.enabled = JDBCUtils.safeGetBoolean(dbResult, "jstenabled");
    }

    @Override
    public long getObjectId() {
        return id;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 2)
    public String getDescription() {
        return description;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public StepKind getKind() {
        return kind;
    }

    @Property(viewable = true, order = 4)
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return job.getDataSource();
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return job.getDatabase();
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return job;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) {
        return code;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        // not implemented
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return enabled ? DBSObjectState.ACTIVE : DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) {
        // not implemented
    }

    public enum StepKind implements DBPNamedObject {
        s("SQL"),
        b("Batch");

        private final String name;

        StepKind(String name) {
            this.name = name;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }
}
