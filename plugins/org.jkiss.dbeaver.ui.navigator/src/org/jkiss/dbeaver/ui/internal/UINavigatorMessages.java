/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.internal;

import org.eclipse.osgi.util.NLS;

public class UINavigatorMessages extends NLS {
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.internal.UINavigatorMessages"; //$NON-NLS-1$

    //object properties editor
    public static String obj_editor_properties_control_action_filter_setting;
    public static String obj_editor_properties_control_action_configure_columns;
    public static String obj_editor_properties_control_action_configure_columns_description;
    //object properties editor

    public static String dialog_filter_global_link;
    public static String dialog_filter_list_exclude;
    public static String dialog_filter_list_include;
    public static String dialog_filter_title;
    public static String dialog_filter_save_button;
    public static String dialog_filter_remove_button;
    public static String dialog_filter_save_label;
    public static String dialog_filter_name_label;
    public static String dialog_filter_hint_text;

    public static String actions_navigator__objects;
    public static String actions_navigator_copy_fqn_title;
    public static String actions_navigator_copy_fqn_titles;
    public static String actions_navigator_copy_object_copy_node;
    public static String actions_navigator_copy_object_copy_objects;
    public static String actions_navigator_create_folder_error_message;
    public static String actions_navigator_create_folder_error_title;
    public static String actions_navigator_create_folder_folder_name;
    public static String actions_navigator_create_new;
    public static String actions_navigator_delete_objects;
    public static String actions_navigator_delete_;
    public static String actions_navigator_delete_script;
    public static String actions_navigator_edit;
    public static String actions_navigator_error_dialog_delete_object_title;
    public static String actions_navigator_error_dialog_open_entity_title;
    public static String actions_navigator_error_dialog_open_resource_title;
    public static String actions_navigator_open;
    public static String actions_navigator_view;
    public static String actions_navigator_view_script_button;
    public static String actions_navigator_filter_objects;
    public static String actions_navigator_search_tip;

    public static String dialog_project_create_wizard_error_already_exists;
    public static String dialog_project_create_wizard_error_cannot_create;
    public static String dialog_project_create_wizard_error_cannot_create_message;
    public static String dialog_project_create_wizard_title;
    public static String dialog_project_goto_object_title;
    
    public static String registry_entity_editor_descriptor_description;
    public static String registry_entity_editor_descriptor_name;
    public static String pref_page_database_general_group_navigator;

    // DatabaseNavigator
    public static String pref_page_database_general_label_expand_navigator_tree;
    public static String pref_page_database_general_label_show_tips_in_tree;
    public static String pref_page_database_general_label_show_tips_in_tree_tip;
    public static String pref_page_database_general_label_order_elements_alphabetically;
    public static String pref_page_database_general_label_folders_first;
    public static String pref_page_database_general_label_folders_first_tip;
    public static String pref_page_database_general_label_group_database_by_driver;
    public static String pref_page_database_general_label_long_list_fetch_size;
    public static String pref_page_database_general_label_long_list_fetch_size_tip;
    public static String pref_page_database_general_label_double_click_node;
    public static String pref_page_database_general_label_double_click_node_open_properties;
    public static String pref_page_database_general_label_double_click_node_expand_collapse;
    public static String pref_page_database_general_label_double_click_connection;
    public static String pref_page_database_general_label_double_click_connection_open_properties;
    public static String pref_page_database_general_label_double_click_connection_conn_disconn;
    public static String pref_page_database_general_label_double_click_connection_open_sqleditor;
    public static String pref_page_database_general_label_double_click_connection_open_new_sqleditor;
    public static String pref_page_database_general_label_double_click_connection_expand_collapse;

    // ProjectSettings
    public static String pref_page_projects_settings_label_resource_location;
    public static String pref_page_projects_settings_label_resource;
    public static String pref_page_projects_settings_label_folder;
    public static String pref_page_projects_settings_label_select;
    public static String pref_page_projects_settings_label_root_folder;
    public static String pref_page_projects_settings_label_not_use_hidden_folders;
    public static String pref_page_projects_settings_label_not_store_resources_in_another_project;
    public static String pref_page_projects_settings_label_restart_require_refresh_global_settings;

    public static String ui_properties_category_information;
    public static String ui_properties_category_information_tip;
    public static String ui_properties_category_properties;
    public static String ui_properties_category_properties_tip;
    public static String ui_properties_category_structure;
    public static String ui_properties_task_add_folder;
    public static String ui_properties_task_add_node;

    public static String CreateLinkHandler_e_create_link_message;
    public static String CreateLinkHandler_e_create_link_title;
    public static String CreateLinkHandler_e_create_link_validation;

    public static String dialog_select_datasource_error_message;
    public static String dialog_select_datasource_error_title;
    public static String dialog_select_datasource_title;

    public static String controls_object_list_job_props_read;
    public static String controls_object_list_message_items;
    public static String controls_object_list_message_no_items;
    public static String controls_object_list_monitor_load_lazy_props;
    public static String controls_object_list_monitor_load_props;
    public static String controls_object_list_status_objects;

    public static String editors_entity_dialog_persist_title;
    public static String editors_entity_dialog_preview_title;
    public static String editors_entity_monitor_add_folder;
    public static String editors_entity_monitor_add_node;
    public static String editors_entity_monitor_preview_changes;
    public static String editors_entity_properties_text;
    public static String editors_entity_properties_tooltip_suffix;

    public static String confirm_entity_delete_title;
    public static String confirm_entity_delete_message;

    public static String confirm_local_folder_delete_title;
    public static String confirm_local_folder_delete_message;

    public static String confirm_entity_reject_title;
    public static String confirm_entity_reject_message;
    public static String confirm_entity_reject_toggleMessage;

    public static String confirm_entity_revert_title;
    public static String confirm_entity_revert_message;
    public static String confirm_entity_revert_toggleMessage;

    public static String confirm_close_editor_edit_title;
    public static String confirm_close_editor_edit_message;

    public static String confirm_close_entity_edit_title;
    public static String confirm_close_entity_edit_message;

    public static String label_configure_columns;

	public static String label_description;

	public static String label_name;

	public static String label_select_columns;

	public static String label_show_all_projects;
	public static String label_show_connected;

	public static String pref_page_target_button_use_datasource_settings;
    public static String pref_page_target_link_show_datasource_settings;
    public static String pref_page_target_link_show_global_settings;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UINavigatorMessages.class);
    }

    private UINavigatorMessages() {
    }
}
