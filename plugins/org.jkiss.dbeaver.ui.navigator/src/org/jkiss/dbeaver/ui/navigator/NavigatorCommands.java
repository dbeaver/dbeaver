/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator;

/**
 * NavigatorCommands
 */
public class NavigatorCommands {

    public static final String GROUP_TOOLS = "tools";
    public static final String GROUP_TOOLS_END = "tools_end";
    public static final String GROUP_NAVIGATOR_ADDITIONS = "navigator_additions";
    public static final String GROUP_NAVIGATOR_ADDITIONS_END = "navigator_additions_end";

    public static final String CMD_OBJECT_OPEN = "org.jkiss.dbeaver.core.object.open"; //$NON-NLS-1$
    public static final String CMD_OBJECT_CREATE = "org.jkiss.dbeaver.core.object.create"; //$NON-NLS-1$
    public static final String CMD_OBJECT_DELETE = "org.jkiss.dbeaver.core.object.delete"; //$NON-NLS-1$
    public static final String CMD_OBJECT_MOVE_UP = "org.jkiss.dbeaver.core.object.move.up"; //$NON-NLS-1$
    public static final String CMD_OBJECT_MOVE_DOWN = "org.jkiss.dbeaver.core.object.move.down"; //$NON-NLS-1$
    public static final String CMD_OBJECT_SET_DEFAULT = "org.jkiss.dbeaver.core.navigator.set.default";
    public static final String CMD_CREATE_LOCAL_FOLDER = "org.jkiss.dbeaver.core.new.folder";
    public static final String CMD_CREATE_RESOURCE_FILE = "org.jkiss.dbeaver.core.resource.create.file";
    public static final String CMD_CREATE_RESOURCE_FOLDER = "org.jkiss.dbeaver.core.resource.create.folder";
    public static final String CMD_CREATE_FILE_LINK = "org.jkiss.dbeaver.core.resource.link.file";
    public static final String CMD_CREATE_FOLDER_LINK = "org.jkiss.dbeaver.core.resource.link.folder";
    public static final String CMD_CREATE_PROJECT = "org.jkiss.dbeaver.core.project.create";

    public static final String PARAM_OBJECT_TYPE = "org.jkiss.dbeaver.core.object.type";
    public static final String PARAM_OBJECT_TYPE_NAME = "org.jkiss.dbeaver.core.object.typeName";
    public static final String PARAM_OBJECT_TYPE_ICON = "org.jkiss.dbeaver.core.object.typeIcon";
    public static final String PARAM_OBJECT_TYPE_FOLDER = "org.jkiss.dbeaver.core.object.folder";

}
