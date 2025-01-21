/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.gis.internal;

import org.jkiss.dbeaver.utils.NLS;

public class GISMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.gis.internal.GISResources"; //$NON-NLS-1$

	public static String panel_leaflet_viewer_tool_bar_action_text_open;
	public static String panel_leaflet_viewer_tool_bar_action_text_copy_as;
	public static String panel_leaflet_viewer_tool_bar_action_text_save_as;
	public static String panel_leaflet_viewer_tool_bar_action_text_print;
	public static String panel_leaflet_viewer_tool_bar_action_text_flip;
	public static String panel_leaflet_viewer_tool_bar_action_tool_tip_text_flip;
	public static String panel_leaflet_viewer_tool_bar_action_text_show_hide;
	
	public static String panel_gis_panel_editor_viewer_action_tool_tip_text_settings;

	public static String pref_page_gis_viewer_group;
	public static String pref_page_gis_viewer_label_srid;
	public static String pref_page_gis_viewer_label_max_objects;
	public static String pref_page_gis_viewer_spinner_min_zoom_level;

	public static String panel_manage_crs_dialog_title_select_system;
	public static String panel_manage_crs_dialog_tree_column_text_name;
	public static String panel_manage_crs_dialog_tree_column_text_srid;
	public static String panel_manage_crs_dialog_tree_column_text_coordinate_system;
	public static String panel_manage_crs_dialog_tree_column_text_projection;
	public static String panel_manage_crs_dialog_monitor_begin_task_load_crs;
	public static String panel_manage_crs_dialog_monitor_sub_task_load_crs;
	
	public static String panel_select_crs_action_menu_manager_other;
	public static String panel_select_crs_action_menu_manager_config;
	
	public static String panel_select_srid_dialog_title;
	public static String panel_select_srid_dialog_label_combo_source_srid;
	public static String panel_select_srid_dialog_label_combo_tooltip_source_crs;
	public static String panel_select_srid_dialog_title_label_text_name;
	public static String panel_select_srid_dialog_button_label_details;
	public static String panel_select_srid_dialog_button_label_manage;
	
	public static String panel_select_tiles_action_text_plain;
	public static String panel_select_tiles_action_no_tiles_selected;
	public static String panel_select_tiles_action_manage_tiles_action;

	public static String panel_set_crs_action_text_simple;
	
	public static String panel_show_srid_dialog_title_select;
	public static String panel_show_srid_dialog_control_group_label_details;
	public static String panel_show_srid_dialog_label_text_name;
	public static String panel_show_srid_dialog_label_text_coordinate;
	public static String panel_show_srid_dialog_label_text_projection;
	public static String panel_show_srid_dialog_label_text_type;

	public static String panel_select_tiles_action_manage_dialog_predefined_tiles;
	public static String panel_select_tiles_action_manage_dialog_user_defined_tiles;
	public static String panel_select_tiles_action_manage_dialog_title;
	public static String panel_select_tiles_action_manage_dialog_tiles_column_name;
	public static String panel_select_tiles_action_manage_dialog_toolbar_add_new_tiles;
	public static String panel_select_tiles_action_manage_dialog_toolbar_view_or_edit_tiles;
	public static String panel_select_tiles_action_manage_dialog_toolbar_delete_tiles;
	public static String panel_select_tiles_action_manage_dialog_error_adding_new_tiles_title;
	public static String panel_select_tiles_action_manage_dialog_error_adding_new_tiles_message;
	public static String panel_select_tiles_action_manage_dialog_error_editing_tiles_title;
	public static String panel_select_tiles_action_manage_dialog_error_editing_tiles_message;
	public static String panel_select_tiles_action_manage_dialog_infolabel_about_checkboxes_meaning;
	public static String panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_add_tiles_title;
	public static String panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_edit_tiles_title;
	public static String panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_view_tiles_title;
	public static String panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_tiles_properties_group;
	public static String panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_text_label_label;
	public static String panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_text_label_layers_definition;
	public static String panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_layers_definition_explanation_link_text;

	public static String panel_configure_labels_action_label;
	public static String panel_show_labels_action_label;
	public static String panel_hide_labels_action_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, GISMessages.class);
	}

	private GISMessages() {
	}
}
