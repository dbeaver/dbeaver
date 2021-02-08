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
package org.jkiss.dbeaver.ui.navigator;

/**
 * NavigatorPreferences
 */
public class NavigatorPreferences {

    public static final String NAVIGATOR_COLOR_ALL_NODES = "navigator.color.nodes.all"; //$NON-NLS-1$
    public static final String NAVIGATOR_EXPAND_ON_CONNECT = "navigator.expand.on.connect"; //$NON-NLS-1$
    public static final String NAVIGATOR_RESTORE_STATE_DEPTH = "navigator.restore.state.depth"; //$NON-NLS-1$
    public static final String NAVIGATOR_SYNC_EDITOR_DATASOURCE = "navigator.sync.editor.datasource"; //$NON-NLS-1$
    public static final String NAVIGATOR_REFRESH_EDITORS_ON_OPEN = "navigator.refresh.editor.open"; //$NON-NLS-1$
    public static final String NAVIGATOR_GROUP_BY_DRIVER = "navigator.group.by.driver"; //$NON-NLS-1$
    public static final String NAVIGATOR_EDITOR_SHOW_TABLE_GRID = "navigator.editor.showGrid"; //$NON-NLS-1$
    public static final String NAVIGATOR_OBJECT_DOUBLE_CLICK = "navigator.object.doubleClick"; //$NON-NLS-1$
    public static final String NAVIGATOR_CONNECTION_DOUBLE_CLICK = "navigator.connection.doubleClick"; //$NON-NLS-1$
    public static final String NAVIGATOR_DEFAULT_EDITOR_PAGE = "navigator.object.defaultEditorPage"; //$NON-NLS-1$
    public static final String NAVIGATOR_SHOW_SQL_PREVIEW = "navigator.editor.show.preview"; //$NON-NLS-1$
    public static final String NAVIGATOR_SHOW_OBJECT_TIPS = "navigator.show.objects.tips"; //$NON-NLS-1$
    public static final String NAVIGATOR_LONG_LIST_FETCH_SIZE = "navigator.long.list.fetch.size"; //$NON-NLS-1$
    public static final String NAVIGATOR_SHOW_STATISTICS_INFO = "navigator.show.statistics.info"; //$NON-NLS-1$
    public static final String NAVIGATOR_SHOW_CONNECTION_HOST_NAME = "navigator.show.connection.host"; //$NON-NLS-1$
    public static final String NAVIGATOR_SHOW_NODE_ACTIONS = "navigator.show.node.actions"; //$NON-NLS-1$

    public static final String NAVIGATOR_SHOW_TOOLTIPS = "navigator.show.tooltips"; //$NON-NLS-1$
    public static final String NAVIGATOR_SHOW_CONTENTS_IN_TOOLTIP = "navigator.show.tooltips.file.contents"; //$NON-NLS-1$

    public static final String ENTITY_EDITOR_DETACH_INFO = "entity.editor.info.detach"; //$NON-NLS-1$
    public static final String ENTITY_EDITOR_INFO_SASH_STATE = "entity.editor.info.sash.state"; //$NON-NLS-1$

    public static final String CONFIRM_LOCAL_FOLDER_DELETE = "local_folder_delete"; //$NON-NLS-1$
    public static final String CONFIRM_ENTITY_DELETE = "entity_delete"; //$NON-NLS-1$
    public static final String CONFIRM_ENTITY_REJECT = "entity_reject"; //$NON-NLS-1$
    public static final String CONFIRM_ENTITY_REVERT = "entity_revert"; //$NON-NLS-1$
    //public static final String CONFIRM_ENTITY_RENAME = "entity_rename"; //$NON-NLS-1$
    public static final String CONFIRM_EDITOR_CLOSE = "close_editor_edit"; //$NON-NLS-1$
    public static final String CONFIRM_ENTITY_EDIT_CLOSE = "close_entity_edit"; //$NON-NLS-1$

    public static final int MIN_LONG_LIST_FETCH_SIZE = 100;

    public enum DoubleClickBehavior {
        EDIT,
        CONNECT,
        SQL_EDITOR,
        EXPAND,
        SQL_EDITOR_NEW
    }
}
