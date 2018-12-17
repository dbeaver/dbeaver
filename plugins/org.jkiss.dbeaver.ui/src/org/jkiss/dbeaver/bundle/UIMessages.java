/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.bundle;

import org.eclipse.osgi.util.NLS;

public class UIMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.bundle.UIMessages"; //$NON-NLS-1$
	
	public static String output_label_directory;

	public static String properties_name;
	public static String properties_value;

	public static String ui_properties_tree_viewer__to_default;
	public static String ui_properties_tree_viewer_action_copy_name;
	public static String ui_properties_tree_viewer_action_copy_value;
	public static String ui_properties_tree_viewer_action_reset_value;
	public static String ui_properties_tree_viewer_category_general;

	public static String sql_editor_resultset_filter_panel_btn_config_refresh;
	public static String sql_editor_resultset_filter_panel_btn_stop_refresh;
	public static String sql_editor_resultset_filter_panel_menu_refresh_interval;
	public static String sql_editor_resultset_filter_panel_menu_refresh_interval_1;
	public static String sql_editor_resultset_filter_panel_menu_stop;
	public static String sql_editor_resultset_filter_panel_menu_customize;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, UIMessages.class);
	}

	private UIMessages() {
	}
}
