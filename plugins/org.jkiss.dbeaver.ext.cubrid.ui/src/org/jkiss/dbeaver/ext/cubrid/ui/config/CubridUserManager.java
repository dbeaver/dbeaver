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
package org.jkiss.dbeaver.ext.cubrid.ui.config;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridDataSource;
import org.jkiss.dbeaver.ext.cubrid.model.CubridPrivilage;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

public class CubridUserManager extends SQLObjectEditor<CubridPrivilage, GenericStructContainer> /*implements DBEObjectRenamer<OracleSchema>*/
{

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, CubridPrivilage> getObjectsCache(CubridPrivilage object) {
        DBSObject parentObject = object.getParentObject();
        if (parentObject instanceof CubridDataSource container) {
            return container.getDataSource().getCubridPrivilageCache();
        }
        return null;
    }

    @Override
    protected CubridPrivilage createDatabaseObject(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBECommandContext context,
            @NotNull final Object container,
            @Nullable Object copyFrom,
            @NotNull Map<String, Object> options) {

        CubridPrivilage user = new CubridPrivilage((CubridDataSource) container, "NEW_USER", null);
        return user;
    }


    @Override
    protected void addObjectCreateActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectCreateCommand command,
            @NotNull Map<String, Object> options) {
        CubridPrivilage user = (CubridPrivilage) command.getObject();
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE USER ");
        builder.append(this.getUserName(user, command.getProperties()));
        buildBody(user, builder, command.getProperties());
        actions.add(new SQLDatabasePersistAction("Create User", builder.toString()));
    }

    @Override
    protected void addObjectDeleteActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectDeleteCommand command,
            @NotNull Map<String, Object> options) {
        StringBuilder builder = new StringBuilder();
        builder.append("DROP USER ");
        builder.append(DBUtils.getQuotedIdentifier(command.getObject()));
        actions.add(new SQLDatabasePersistAction("Drop User", builder.toString()));
    }

    private void buildBody(CubridPrivilage user, StringBuilder builder, Map<Object, Object> properties) {
        Object password = properties.get("PASSWORD");
        Object description = properties.get("DESCRIPTION");
        Object group = properties.get("GROUPS");
        if (password != null && CommonUtils.isNotEmpty(password.toString())) {
            builder.append(" PASSWORD ");
            builder.append(SQLUtils.quoteString(user, password.toString()));
        }
        if (group != null && !CommonUtils.isEmpty((List<String>) properties.get("GROUPS"))) {
            builder.append(" GROUPS ");
            builder.append(String.join(", ", (List<String>) properties.get("GROUPS")));
        }

        if (description != null && CommonUtils.isNotEmpty(description.toString())) {
            builder.append(" COMMENT ");
            builder.append(SQLUtils.quoteString(user, description.toString()));
        }
    }

    @NotNull
    private String getUserName(CubridPrivilage user, Map<Object, Object> properties) {
        Object name = properties.get(CubridPrivilageHandler.NAME.getId());
        if (name != null) {
            user.setName(name.toString());
        }
        return user.getName();

    }


}