/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2020 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolConsumerGroup;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExasolConsumerGroupManager extends SQLObjectEditor<ExasolConsumerGroup, ExasolDataSource> implements DBEObjectRenamer<ExasolConsumerGroup> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<ExasolDataSource, ExasolConsumerGroup> getObjectsCache(ExasolConsumerGroup object) {
        return object.getDataSource().getConsumerGroupCache();
    }

    @Override
    protected ExasolConsumerGroup createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context,
                                                       Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        return new ExasolConsumerGroup(
            (ExasolDataSource) container,
            "PG",
            null,
            1,
            null,
            null,
            null,
            null);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
                                          @NotNull ObjectCreateCommand command,
                                          @NotNull Map<String, Object> options) {
        final ExasolConsumerGroup group = command.getObject();

        String script = String.format("CREATE CONSUMER GROUP %s WITH CPU_WEIGHT = %d", DBUtils.getQuotedIdentifier(group), group.getCpuWeight());

        if (group.getGroupRamLimit() != null)
            script += String.format(",GROUP_TEMP_DB_RAM_LIMIT=%d", group.getGroupRamLimit().longValue());
        if (group.getUserRamLimit() != null)
            script += String.format(",USER_TEMP_DB_RAM_LIMIT=%d", group.getUserRamLimit().longValue());
        if (group.getSessionRamLimit() != null)
            script += String.format(",SESSION_TEMP_DB_RAM_LIMIT=%d", group.getSessionRamLimit().longValue());
        if (group.getPrecedence() != null)
            script += String.format(",PRECEDENCE=%d", group.getPrecedence());


        actions.add(new SQLDatabasePersistAction(ExasolMessages.manager_consumer_create, script));

        if (CommonUtils.isNotEmpty(group.getDescription())) {
            actions.add(getCommentCommand(group));
        }
    }

    private SQLDatabasePersistAction getCommentCommand(ExasolConsumerGroup group) {
        return new SQLDatabasePersistAction(
            ExasolMessages.manager_priority_group_comment,
            String.format("COMMENT ON CONSUMER GROUP %s is '%s'",
                DBUtils.getQuotedIdentifier(group),
                ExasolUtils.quoteString(group.getDescription())
            )
        );
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList,
                                          @NotNull ObjectChangeCommand command,
                                          @NotNull Map<String, Object> options) throws DBException {
        ExasolConsumerGroup group = command.getObject();

        Map<Object, Object> com = command.getProperties();

        if (com.containsKey("description")) {
            actionList.add(
                getCommentCommand(group)
            );
        }

        List<String> alters = new ArrayList<String>();

        if (com.containsKey("cpuWeight"))
            alters.add(String.format("CPU_WEIGHT=%d", group.getCpuWeight()));
        if (com.containsKey("precedence"))
            alters.add(String.format("PRECEDENCE=%d", group.getPrecedence()));
        if (com.containsKey("groupRamLimit"))
            alters.add(String.format("GROUP_TEMP_DB_RAM_LIMIT=%d", group.getGroupRamLimit().longValue()));
        if (com.containsKey("userRamLimit"))
            alters.add(String.format("USER_TEMP_DB_RAM_LIMIT=%d", group.getUserRamLimit().longValue()));
        if (com.containsKey("sessionRamLimit"))
            alters.add(String.format("SESSION_TEMP_DB_RAM_LIMIT=%d", group.getSessionRamLimit().longValue()));

        if (alters.size() > 0) {
            String modifyPart = String.join(", ", alters);
            actionList.add(
                new SQLDatabasePersistAction(ExasolMessages.manager_consumer_alter,
                    String.format("ALTER CONSUMER GROUP %s SET %s", DBUtils.getQuotedIdentifier(group), modifyPart)
                )
            );
        }

    }


    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
                                          @NotNull ObjectDeleteCommand command,
                                          @NotNull Map<String, Object> options) {

        ExasolConsumerGroup group = command.getObject();

        String script = String.format("DROP CONSUMER GROUP %s", DBUtils.getQuotedIdentifier(group));

        actions.add(new SQLDatabasePersistAction(ExasolMessages.manager_consumer_drop, script));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull ExasolConsumerGroup object, @NotNull Map<String, Object> options, @NotNull String newName)
        throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
                                          @NotNull ObjectRenameCommand command,
                                          @NotNull Map<String, Object> options) {
        ExasolConsumerGroup group = command.getObject();

        String script = String.format(
            "RENAME CONSUMER GROUP %s to %s",
            DBUtils.getQuotedIdentifier(group.getDataSource(), command.getOldName()),
            DBUtils.getQuotedIdentifier(group.getDataSource(), command.getNewName())
        );
        actions.add(new SQLDatabasePersistAction(ExasolMessages.manager_consumer_rename, script));
    }


}
