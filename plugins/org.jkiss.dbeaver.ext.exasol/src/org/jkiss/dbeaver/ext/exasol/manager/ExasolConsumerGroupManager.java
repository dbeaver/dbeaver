/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolConsumerGroup;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolConsumerGroupDialog;
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
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExasolConsumerGroupManager extends SQLObjectEditor<ExasolConsumerGroup, ExasolDataSource> implements DBEObjectRenamer<ExasolConsumerGroup> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<ExasolDataSource, ExasolConsumerGroup> getObjectsCache(ExasolConsumerGroup object) {
        return object.getDataSource().getConsumerGroupCache();
    }

    @Override
    protected ExasolConsumerGroup createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                                       Object container, Object copyFrom, Map<String, Object> options) throws DBException {
    	ExasolConsumerGroup group = new ExasolConsumerGroup((ExasolDataSource) container, "PG", null, null, null, null, null, null);
        return new UITask<ExasolConsumerGroup>() {
            @Override
            protected ExasolConsumerGroup runTask() {
            	ExasolConsumerGroupDialog dialog = new ExasolConsumerGroupDialog(UIUtils.getActiveWorkbenchShell(), group);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                group.setName(dialog.getName());
                group.setDescription(dialog.getComment());
                group.setCpuWeight(dialog.getCpuWeight());
                group.setSessionRamLimit(dialog.getSessionRamLimit());
                group.setUserRamLimit(dialog.getUserRamLimit());
                group.setGroupRamLimit(dialog.getGroupRamLimit());
                return group;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command,
                                          Map<String, Object> options) {
        final ExasolConsumerGroup group = command.getObject();

        String script = String.format("CREATE CONSUMER GROUP %s WITH CPU_WEIGHT = %d", DBUtils.getQuotedIdentifier(group), group.getCpuWeight());
        
        if (group.getGroupRamLimit() != null)
        	script+=String.format(",GROUP_TEMP_DB_RAM_LIMIT=%d", group.getGroupRamLimit().longValue());
        if (group.getUserRamLimit() != null)
        	script+=String.format(",USER_TEMP_DB_RAM_LIMIT=%d", group.getUserRamLimit().longValue());
        if (group.getSessionRamLimit() != null)
        	script+=String.format(",SESSION_TEMP_DB_RAM_LIMIT=%d", group.getSessionRamLimit().longValue());
        if (group.getPrecedence() != null)
        	script+=String.format(",PRECEDENCE=%d", group.getPrecedence().intValue());
        

        actions.add(new SQLDatabasePersistAction(ExasolMessages.manager_consumer_create, script));

        if (!group.getDescription().isEmpty()) {
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
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command,
                                          Map<String, Object> options) throws DBException {
    	ExasolConsumerGroup group = command.getObject();

        Map<Object, Object> com = command.getProperties();

        if (com.containsKey("description")) {
            actionList.add(
                getCommentCommand(group)
            );
        }
        
        List<String> alters = new ArrayList<String>(); 

    	if (com.containsKey("cpuWeight"))
    		alters.add(String.format("CPU_WEIGHT=%d",group.getCpuWeight()));
    	if (com.containsKey("precedence"))
    		alters.add(String.format("PRECEDENCE=%d",group.getPrecedence().intValue()));
    	if (com.containsKey("groupRamLimit"))
    		alters.add(String.format("GROUP_TEMP_DB_RAM_LIMIT=%d",group.getGroupRamLimit().longValue()));
    	if (com.containsKey("userRamLimit"))
    		alters.add(String.format("USER_TEMP_DB_RAM_LIMIT=%d",group.getUserRamLimit().longValue()));
    	if (com.containsKey("sessionRamLimit"))
    		alters.add(String.format("SESSION_TEMP_DB_RAM_LIMIT=%d",group.getSessionRamLimit().longValue()));
    	
    	if (alters.size() >0) {
    		String modifyPart = alters.stream().collect(Collectors.joining(", "));
    		actionList.add(
    				new SQLDatabasePersistAction(ExasolMessages.manager_consumer_alter,
    						String.format("ALTER CONSUMER GROUP %s SET %s",DBUtils.getQuotedIdentifier(group), modifyPart)
    						)
    				);
    	}
        	
    }
    

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectDeleteCommand command,
                                          Map<String, Object> options) {

    	ExasolConsumerGroup group = command.getObject();

        String script = String.format("DROP CONSUMER GROUP %s", DBUtils.getQuotedIdentifier(group));

        actions.add(new SQLDatabasePersistAction(ExasolMessages.manager_consumer_drop, script));
    }

    @Override
    public void renameObject(DBECommandContext commandContext, ExasolConsumerGroup object, String newName)
        throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectRenameCommand command,
                                          Map<String, Object> options) {
        // TODO Auto-generated method stub
    	ExasolConsumerGroup group = command.getObject();

        String script = String.format(
            "RENAME CONSUMER GROUP %s to %s",
            DBUtils.getQuotedIdentifier(group.getDataSource(), command.getOldName()),
            DBUtils.getQuotedIdentifier(group.getDataSource(), command.getNewName())
        );
        actions.add(new SQLDatabasePersistAction(ExasolMessages.manager_consumer_rename, script));
    }


}
