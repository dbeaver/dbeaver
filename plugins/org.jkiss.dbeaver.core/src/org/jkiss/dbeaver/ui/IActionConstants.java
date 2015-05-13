/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui;

/**
 * Action constants
 */
public interface IActionConstants {

    public static final String LOG_VIEW_ID = "org.eclipse.pde.runtime.LogView"; //$NON-NLS-1$
    public static final String HELP_VIEW_ID = "org.eclipse.help.ui.HelpView"; //$NON-NLS-1$

    public static final String M_DATABASE = "dataSourceMenu"; //$NON-NLS-1$
    public static final String M_DRIVER_GROUP = "driverGroup"; //$NON-NLS-1$
    public static final String M_CONNECTION_GROUP = "connectionGroup"; //$NON-NLS-1$
    public static final String M_TOOLS_GROUP = "toolsGroup"; //$NON-NLS-1$
    public static final String M_SESSION_GROUP = "sessionGroup"; //$NON-NLS-1$

    public static final String TOOLBAR_DATABASE = "database"; //$NON-NLS-1$
    public static final String TOOLBAR_TXN = "txn"; //$NON-NLS-1$
    public static final String TOOLBAR_DATASOURCE = "datasource-settings"; //$NON-NLS-1$
    //public static final String MENU_ID = "org.jkiss.dbeaver.core.navigationMenu";

    public static final String MB_ADDITIONS_END = "additions_end"; //$NON-NLS-1$
    public static final String MB_ADDITIONS_PROPS = "additions_props"; //$NON-NLS-1$
    public static final String MB_ADDITIONS_MIDDLE = "additions_middle"; //$NON-NLS-1$
    public static final String MB_ADDITIONS_START = "additions_start"; //$NON-NLS-1$

    public static final String NEW_CONNECTION_POINT = "org.jkiss.dbeaver.ext.ui.newConnectionWizard"; //$NON-NLS-1$
    public static final String EDIT_CONNECTION_POINT = "org.jkiss.dbeaver.ext.ui.editConnectionDialog"; //$NON-NLS-1$
}
