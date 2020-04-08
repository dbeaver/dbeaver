/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.internal;

import org.eclipse.osgi.util.NLS;

public class TaskUIMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages"; //$NON-NLS-1$

	public static String task_config_wizard_group_task_label;
	public static String task_config_wizard_button_save_task;
	public static String task_config_wizard_link_open_tasks_view;
	public static String task_config_wizard_button_variables;
	
	public static String task_config_wizard_dialog_button_save;
	
	public static String task_config_wizard_page_settings_create_task;
	public static String task_config_wizard_page_settings_edit_task;
	public static String task_config_wizard_page_settings_title_task_prop;
	public static String task_config_wizard_page_settings_descr_set_task;
	public static String task_config_wizard_page_settings_config;
	public static String task_config_wizard_page_settings_settings;
	
	

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, TaskUIMessages.class);
	}

	private TaskUIMessages() {
	}
}
