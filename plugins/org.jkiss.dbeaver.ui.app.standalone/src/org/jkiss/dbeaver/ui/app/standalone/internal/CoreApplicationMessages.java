/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.app.standalone.internal;

import org.eclipse.osgi.util.NLS;

public class CoreApplicationMessages extends NLS {
	public static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages"; //$NON-NLS-1$
	
	public static String actions_menu_exit_emergency;
    public static String actions_menu_exit_emergency_message;
	public static String actions_menu_recent_editors;

    public static String reset_ui_settings_confirmation_title;
    public static String reset_ui_settings_confirmation_message;

    public static String clear_history_dialog_title;
    public static String clear_history_dialog_message;
    public static String clear_history_dialog_options;
    public static String clear_history_dialog_apply_and_restart;
    public static String clear_history_error_title;
    public static String clear_history_error_message;

	public static String confirmation_cancel_database_tasks_title;
	public static String confirmation_cancel_database_tasks_message;

    static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CoreApplicationMessages.class);
	}

	private CoreApplicationMessages() {
	}
}
