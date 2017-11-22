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

package org.jkiss.dbeaver.ext.erd;

import org.eclipse.osgi.util.NLS;

public class ERDMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.erd.ERDResources"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ERDMessages.class);
	}

    public static String action_diagram_layout_name;
	public static String column_;
    public static String entity_diagram_;
    public static String part_note_title;
	public static String pref_page_erd_checkbox_grid_enabled;
	public static String pref_page_erd_checkbox_snap_to_grid;
    public static String pref_page_erd_combo_page_mode;
	public static String pref_page_erd_group_grid;
	public static String pref_page_erd_group_print;
	public static String pref_page_erd_item_fit_height;
	public static String pref_page_erd_item_fit_page;
	public static String pref_page_erd_item_fit_width;
	public static String pref_page_erd_item_tile;
	public static String pref_page_erd_spinner_grid_height;
	public static String pref_page_erd_spinner_grid_width;
	public static String pref_page_erd_spinner_margin_bottom;
	public static String pref_page_erd_spinner_margin_left;
	public static String pref_page_erd_spinner_margin_right;
	public static String pref_page_erd_spinner_margin_top;

    public static String wizard_diagram_create_title;

	public static String wizard_page_diagram_create_description;
	public static String wizard_page_diagram_create_group_settings;
	public static String wizard_page_diagram_create_label_init_content;
	public static String wizard_page_diagram_create_name;
	public static String wizard_page_diagram_create_title;

	//ERD editor action item control
	public static String erd_editor_control_action_toggle_grid;
	public static String erd_editor_control_action_refresh_diagram;
	public static String erd_editor_control_action_save_external_format;
	public static String erd_editor_control_action_print_diagram;
	public static String erd_editor_control_action_configuration;
	//ERD editor action item control
	
	
	
	private ERDMessages() {
	}
}
