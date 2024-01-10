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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostgreJobStep implements PostgreObject, PostgreScriptObject, DBPNamedObject2, DBPRefreshableObject, DBPSaveableObject, DBPStatefulObject {
    private final PostgreJob job;
    private final long id;
    private String name;
    private String description;
    private String code;
    private StepKind kind;
    private ActionKind onError;
    private String remoteConnectionString;
    private PostgreDatabase targetDatabase;
    private boolean enabled;
    private boolean persisted;

    public PostgreJobStep(@NotNull PostgreJob job, @NotNull ResultSet dbResult) {
        this.job = job;
        this.id = JDBCUtils.safeGetInt(dbResult, "jstid");
        this.name = JDBCUtils.safeGetString(dbResult, "jstname");
        this.description = JDBCUtils.safeGetString(dbResult, "jstdesc");
        this.code = JDBCUtils.safeGetString(dbResult, "jstcode");
        this.kind = CommonUtils.valueOf(StepKind.class, JDBCUtils.safeGetString(dbResult, "jstkind"));
        this.onError = CommonUtils.valueOf(ActionKind.class, JDBCUtils.safeGetString(dbResult, "jstonerror"));
        this.remoteConnectionString = JDBCUtils.safeGetString(dbResult, "jstconnstr");
        this.targetDatabase = getDataSource().getDatabase(JDBCUtils.safeGetString(dbResult, "jstdbname"));
        this.enabled = JDBCUtils.safeGetBoolean(dbResult, "jstenabled");
        this.persisted = true;
    }

    public PostgreJobStep(@NotNull PostgreJob job, @NotNull String name) {
        this.job = job;
        this.id = 0;
        this.name = name;
        this.description = "";
        this.code = "";
        this.kind = StepKind.s;
        this.onError = ActionKind.f;
        this.remoteConnectionString = "";
        this.targetDatabase = job.getDatabase();
        this.enabled = true;
        this.persisted = false;
    }

    @Override
    public long getObjectId() {
        return id;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 2)
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public StepKind getKind() {
        return kind;
    }

    public void setKind(@NotNull StepKind kind) {
        this.kind = kind;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 4)
    public ActionKind getOnError() {
        return onError;
    }

    public void setOnError(@NotNull ActionKind onError) {
        this.onError = onError;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 5)
    public String getRemoteConnectionString() {
        return remoteConnectionString;
    }

    public void setRemoteConnectionString(@NotNull String remoteConnectionString) {
        this.remoteConnectionString = remoteConnectionString;
    }

    @Nullable
    @Property(viewable = true, editable = true, updatable = true, order = 6, listProvider = DatabaseListProvider.class)
    public PostgreDatabase getTargetDatabase() {
        return targetDatabase;
    }

    public void setTargetDatabase(@Nullable PostgreDatabase targetDatabase) {
        this.targetDatabase = targetDatabase;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 7)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
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

    @NotNull
    @Override
    public PostgreJob getParentObject() {
        return job;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) {
        return code;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        this.code = source;
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return job.getStepCache().refreshObject(monitor, job, this);
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

        StepKind(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }

    public enum ActionKind implements DBPNamedObject {
        f("Fail"),
        s("Success"),
        i("Ignore");

        private final String name;

        ActionKind(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }

    public static class DatabaseListProvider implements IPropertyValueListProvider<PostgreJobStep> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(PostgreJobStep object) {
            final List<PostgreDatabase> objects = new ArrayList<>(object.getDataSource().getDatabases());
            objects.add(null);
            return objects.toArray();
        }
    }
}
