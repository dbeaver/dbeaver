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

package org.jkiss.dbeaver;

import org.jkiss.dbeaver.ui.editors.DatabaseEditorPreferences;

/**
 * Preferences constants
 */
public final class DBeaverPreferences
{
    public static final String AGENT_ENABLED = "agent.enabled"; //$NON-NLS-1$
    public static final String AGENT_LONG_OPERATION_NOTIFY = "agent.long.operation.notify"; //$NON-NLS-1$
    public static final String AGENT_LONG_OPERATION_TIMEOUT = "agent.long.operation.timeout"; //$NON-NLS-1$

    public static final String SECURITY_USE_BOUNCY_CASTLE = "security.jce.bc"; //$NON-NLS-1$

    public static final String TOOLBARS_SHOW_GENERAL_ALWAYS = "toolbars.show.general.always"; //$NON-NLS-1$
    public static final String TOOLBARS_SHOW_EDIT = "toolbars.show.edit"; //$NON-NLS-1$

    public static final String TEXT_EDIT_UNDO_LEVEL = "text.edit.undo.level"; //$NON-NLS-1$

    public static final String CONFIRM_EXIT = "exit"; //$NON-NLS-1$
    public static final String CONFIRM_TXN_DISCONNECT = "disconnect_txn"; //$NON-NLS-1$
    public static final String CONFIRM_DRIVER_DOWNLOAD = "driver_download"; //$NON-NLS-1$
    public static final String CONFIRM_MANUAL_DOWNLOAD = "driver_download_manual"; //$NON-NLS-1$
    public static final String CONFIRM_VERSION_CHECK = "version_check"; //$NON-NLS-1$

    public static final String NAVIGATOR_EDITOR_FULL_NAME = DatabaseEditorPreferences.PROP_TITLE_SHOW_FULL_NAME; //$NON-NLS-1$

    private static final String PROPERTY_USE_ALL_COLUMNS_QUIET = "virtual-key-quiet";

    // General UI
    public static final String UI_AUTO_UPDATE_CHECK = "ui.auto.update.check"; //$NON-NLS-1$
    public static final String UI_UPDATE_CHECK_TIME = "ui.auto.update.check.time"; //$NON-NLS-1$
    public static final String UI_KEEP_DATABASE_EDITORS = DatabaseEditorPreferences.PROP_SAVE_EDITORS_STATE; //$NON-NLS-1$

    // Resources
    public static final String RESOURCE_HANDLER_ROOT_PREFIX = "resource.root."; //$NON-NLS-1$

    //public static final String DEFAULT_RESOURCE_ENCODING = "resource.encoding.default";

    public static final String LOGS_DEBUG_ENABLED = "logs.debug.enabled";
    public static final String LOGS_DEBUG_LOCATION = "logs.debug.location";

}
