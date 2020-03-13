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

package org.jkiss.dbeaver.ui;

/**
 * Action constants
 */
public interface IActionConstants {

    String LOG_VIEW_ID = "org.eclipse.pde.runtime.LogView"; //$NON-NLS-1$
    String HELP_VIEW_ID = "org.eclipse.help.ui.HelpView"; //$NON-NLS-1$

    String M_DATABASE = "dataSourceMenu"; //$NON-NLS-1$
    String M_DRIVER_GROUP = "driverGroup"; //$NON-NLS-1$
    String M_CONNECTION_GROUP = "connectionGroup"; //$NON-NLS-1$
    String M_TOOLS_GROUP = "toolsGroup"; //$NON-NLS-1$
    String M_SESSION_GROUP = "sessionGroup"; //$NON-NLS-1$

    String TOOLBAR_DATABASE = "dbeaver-general"; //$NON-NLS-1$
    String TOOLBAR_TXN = "dbeaver-transactions"; //$NON-NLS-1$
    String TOOLBAR_DATASOURCE = "datasource-settings"; //$NON-NLS-1$
    String TOOLBAR_EDIT = "dbeaver-edit"; //$NON-NLS-1$
    //String MENU_ID = "org.jkiss.dbeaver.core.navigationMenu";

    String MB_ADDITIONS_END = "additions_end"; //$NON-NLS-1$
    String MB_ADDITIONS_PROPS = "additions_props"; //$NON-NLS-1$
    String MB_ADDITIONS_MIDDLE = "additions_middle"; //$NON-NLS-1$
    String MB_ADDITIONS_START = "additions_start"; //$NON-NLS-1$

    String NEW_CONNECTION_POINT = "org.jkiss.dbeaver.ext.ui.newConnectionWizard"; //$NON-NLS-1$
    String EDIT_CONNECTION_POINT = "org.jkiss.dbeaver.ext.ui.editConnectionDialog"; //$NON-NLS-1$

    String CMD_COPY_SPECIAL = "org.jkiss.dbeaver.core.edit.copy.special"; //$NON-NLS-1$
    String CMD_PASTE_SPECIAL = "org.jkiss.dbeaver.core.edit.paste.special"; //$NON-NLS-1$
}
