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

package org.jkiss.dbeaver.tools.registry;

import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.registry.task.TaskTypeDescriptor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ToolDescriptor
 */
public class ToolDescriptor extends AbstractDescriptor {
    private final String id;
    private final String label;
    private final String description;
    private final DBPImage icon;
    private final boolean singleton;
    private final ToolGroupDescriptor group;

    private final Set<String> toolImplTaskIds;
    private final Set<ToolCommandRef> toolCommandRefs;

    public ToolDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.singleton = CommonUtils.toBoolean(config.getAttribute(RegistryConstants.ATTR_SINGLETON));
        String groupId = config.getAttribute(RegistryConstants.ATTR_GROUP);
        this.group = CommonUtils.isEmpty(groupId) ? null : ToolsRegistry.getInstance().getToolGroup(groupId);
        this.toolImplTaskIds = Stream.of(config.getChildren("task"))
            .map(e -> e.getAttribute(RegistryConstants.ATTR_ID)).collect(Collectors.toSet());
        this.toolCommandRefs = Stream.of(config.getChildren("command"))
            .map(e -> new ToolCommandRef(e)).collect(Collectors.toSet());
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public DBPImage getIcon() {
        return icon;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public ToolGroupDescriptor getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return id + " (" + label + ")";
    }

    /**
     * Return task for the selected command
     *
     * @param objects - selected objects
     * @return corresponding task or null
     */
    @Nullable
    public TaskTypeDescriptor getTaskForObjects(@NotNull Collection<DBSObject> objects) {
        for (String taskId : toolImplTaskIds) {
            TaskTypeDescriptor task = TaskRegistry.getInstance().getTaskType(taskId);
            if (task != null && objects.stream().allMatch(task::appliesTo)) {
                return task;
            }
        }
        return null;
    }
    

    /**
     * Return command for the selected command
     *
     * @param objects - selected objects
     * @return corresponding command or null
     */
    @Nullable
    public Command getCommandForObjects(@NotNull Collection<DBSObject> objects) {
        for (ToolCommandRef cmdRef : toolCommandRefs) {
            if (objects.stream().allMatch(cmdRef::appliesTo)) {
                return cmdRef.getCommand();
            }
        }
        return null;
    }

    /**
     * Checks if the tool tasks could be applied to the given object
     *
     * @param item selected object
     * @return indication of the possibility of application
     */
    public boolean appliesTo(@NotNull DBPObject item) {
        for (String taskId : toolImplTaskIds) {
            TaskTypeDescriptor task = TaskRegistry.getInstance().getTaskType(taskId);
            if (task != null && task.appliesTo(item)) {
                return true;
            }
        }
        for (ToolCommandRef cmdRef : toolCommandRefs) {
            if (cmdRef.appliesTo(item)) {
                return true;
            }
        }
        return false;
    }
    
    private static class ToolCommandRef extends AbstractContextDescriptor {
        public final String commandId;
        private Command command = null;
        
        public ToolCommandRef(@NotNull IConfigurationElement e) {
            super(e);
            this.commandId = e.getAttribute(RegistryConstants.ATTR_ID);
        }

        @Nullable
        public Command getCommand() {
            return command != null ? command : (command = ActionUtils.findCommand(commandId));
        }
        
        @Override
        protected Object adaptType(DBPObject object) {
            if (object instanceof DBSObject) {
                return ((DBSObject) object).getDataSource();
            }
            return super.adaptType(object);
        }
    }
}
