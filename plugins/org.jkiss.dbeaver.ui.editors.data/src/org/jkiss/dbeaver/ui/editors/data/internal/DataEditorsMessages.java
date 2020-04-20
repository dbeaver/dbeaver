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
package org.jkiss.dbeaver.ui.editors.data.internal;

import org.eclipse.osgi.util.NLS;

public class DataEditorsMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsResources"; //$NON-NLS-1$

	public static String grid_tooltip_sort_by_column;
	public static String grid_tooltip_filter_by_column;
    public static String controls_column_info_panel_property_key;
	public static String resultset_segment_size;
	
	public static String virtual_structure_editor_abstract_job_load_entity;
	public static String virtual_structure_editor_info_label_entity_structure;
	public static String virtual_structure_editor_dictionary_page_text;
	public static String virtual_structure_editor_columns_group_virtual;
	public static String virtual_structure_editor_columns_group_unique_keys;
	public static String virtual_structure_editor_table_column_key_name;
	public static String virtual_structure_editor_table_column_columns;
	public static String virtual_structure_editor_dialog_button_add;
	public static String virtual_structure_editor_constraint_page_edit_key;
	public static String virtual_structure_editor_dialog_button_edit;
	public static String virtual_structure_editor_dialog_button_remove;
	public static String virtual_structure_editor_confirm_action_delete_key;
	public static String virtual_structure_editor_confirm_action_question_delete;
	public static String virtual_structure_editor_control_group_label_foreign_key;
	public static String virtual_structure_editor_table_column_target_table;
	public static String virtual_structure_editor_table_column_datasource;
	public static String virtual_structure_editor_confirm_action_delete_fk;
	public static String virtual_structure_editor_confirm_action_question_delete_foreign;
	public static String virtual_structure_editor_control_group_references;
	public static String virtual_structure_editor_table_column_source_table;
	public static String virtual_structure_editor_table_column_source_datasource;
	public static String virtual_structure_editor_dialog_button_refresh;
	

    static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DataEditorsMessages.class);
	}

	private DataEditorsMessages() {
	}
}
