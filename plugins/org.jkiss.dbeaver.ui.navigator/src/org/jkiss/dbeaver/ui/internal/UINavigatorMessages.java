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
package org.jkiss.dbeaver.ui.internal;

import org.eclipse.osgi.util.NLS;

public class UINavigatorMessages extends NLS {
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.internal.UINavigatorMessages"; //$NON-NLS-1$

    //object properties editor
    public static String obj_editor_properties_control_action_filter_setting;
    public static String obj_editor_properties_control_action_configure_columns;
    public static String obj_editor_properties_control_action_configure_columns_description;
    public static String obj_editor_properties_control_action_columns_fit_width;
    public static String obj_editor_properties_control_action_columns_fit_width_description;
    //object properties editor

    public static String toolbar_datasource_selector_empty;
    public static String toolbar_datasource_selector_action_read_databases;
    public static String toolbar_datasource_selector_combo_database_tooltip;
    public static String toolbar_datasource_selector_combo_datasource_tooltip;
    public static String toolbar_datasource_selector_error_change_database_message;
    public static String toolbar_datasource_selector_error_change_database_title;
    public static String toolbar_datasource_selector_error_database_not_found;
    public static String toolbar_datasource_selector_error_database_change_not_supported;
    public static String toolbar_datasource_selector_resultset_segment_size;
    public static String toolbar_datasource_selector_connected;
    public static String toolbar_datasource_selector_all;

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
    public static String actions_navigator_hide_objects;
    public static String actions_navigator_hide_object;
    public static String actions_navigator_copy_fqn_title;
    public static String actions_navigator_copy_fqn_titles;
    public static String actions_navigator_copy_object_copy_node;
    public static String actions_navigator_copy_object_copy_objects;
    public static String actions_navigator_create_folder_error_message;
    public static String actions_navigator_create_folder_error_title;
    public static String actions_navigator_create_folder_folder_name;
    public static String actions_navigator_create_new;
    public static String actions_navigator_delete_objects;
    public static String actions_navigator_delete;
    public static String actions_navigator_delete_script;
    public static String actions_navigator_edit;
    public static String actions_navigator_persist_delete_in_the_editor_title;
    public static String actions_navigator_persist_delete_in_the_editor_message;
    public static String actions_navigator_error_dialog_delete_object_title;
    public static String actions_navigator_error_dialog_delete_object_message;
    public static String actions_navigator_error_dialog_open_entity_title;
    public static String actions_navigator_error_dialog_open_resource_title;
    public static String actions_navigator_open;
    public static String actions_navigator_view;
    public static String actions_navigator_view_script_button;
    public static String actions_navigator_filter_objects;
    public static String actions_navigator_search_tip;
    public static String actions_navigator_search_filter_connection_name;
    public static String actions_navigator_search_filter_connection_description;
    public static String actions_navigator_search_filter_container_name;
    public static String actions_navigator_search_filter_container_description;
    public static String actions_navigator_search_filter_object_name;
    public static String actions_navigator_search_filter_object_description;
    public static String actions_navigator_show_only_selected_objects;
    public static String actions_navigator_show_only_object;
    public static String actions_navigator_folder_name;
    public static String actions_navigator_open_editors_title;
    public static String actions_navigator_open_editors_question;
    public static String actions_navigator_rename_object;
    public static String actions_navigator_rename_object_exception_title;
    public static String actions_navigator_rename_object_exception_message;
    public static String actions_navigator_rename_script;
    public static String actions_navigator_rename_database_object;
    public static String actions_navigator_rename_database_object_exception_title;
    public static String actions_navigator_rename_database_object_exception_message;
    

    public static String dialog_project_create_wizard_error_already_exists;
    public static String dialog_project_create_wizard_error_cannot_create;
    public static String dialog_project_create_wizard_error_cannot_create_message;
    public static String dialog_project_create_wizard_title;
    public static String dialog_project_goto_object_title;
    public static String dialog_project_goto_object_checkbox_search_in_comments;
    
    public static String registry_entity_editor_descriptor_description;
    public static String registry_entity_editor_descriptor_name;
    public static String pref_page_database_general_group_navigator;
    public static String pref_page_database_navigator_group_misc;
    public static String pref_page_database_navigator_group_behavior;

    // DatabaseNavigator
    public static String pref_page_database_general_label_expand_navigator_tree;
    public static String pref_page_database_general_label_expand_navigator_tree_tip;
    public static String pref_page_database_general_label_restore_filter;
    public static String pref_page_database_general_label_restore_filter_tip;
    public static String pref_page_database_general_label_restore_state_depth;
    public static String pref_page_database_general_label_restore_state_depth_tip;
    public static String pref_page_database_general_label_show_tips_in_tree;
    public static String pref_page_database_general_label_show_tips_in_tree_tip;
    public static String pref_page_database_general_label_show_tooltips;
    public static String pref_page_database_general_label_show_tooltips_tip;
    public static String pref_page_database_general_label_show_contents_in_tooltips;
    public static String pref_page_database_general_label_show_contents_in_tooltips_tip;
    public static String pref_page_database_general_label_order_elements_alphabetically;
    public static String pref_page_database_general_label_order_elements_alphabetically_tip;
    public static String pref_page_database_general_label_folders_first;
    public static String pref_page_database_general_label_folders_first_tip;
    public static String pref_page_database_general_label_show_host_name;
    public static String pref_page_database_general_label_show_host_name_tip;
    public static String pref_page_database_general_label_show_objects_description;
    public static String pref_page_database_general_label_show_objects_description_tip;
    public static String pref_page_database_general_label_show_statistics;
    public static String pref_page_database_general_label_show_statistics_tip;
    public static String pref_page_database_general_label_show_node_actions;
    public static String pref_page_database_general_label_show_node_actions_tip;
    public static String pref_page_database_general_label_color_all_nodes;
    public static String pref_page_database_general_label_color_all_nodes_tip;
    public static String pref_page_database_general_label_show_folder_placeholders;
    public static String pref_page_database_general_label_show_folder_placeholders_tip;
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
    public static String pref_page_navigator_default_editor_page_label;
    public static String pref_page_navigator_default_editor_page_tip;
    public static String pref_page_navigator_default_editor_page_last;


    // ProjectSettings
    public static String pref_page_projects_settings_label_resource_location;
    public static String pref_page_projects_settings_label_resource;
    public static String pref_page_projects_settings_label_folder;
    public static String pref_page_projects_settings_label_select;
    public static String pref_page_projects_settings_label_root_folder;
    public static String pref_page_projects_settings_label_not_use_project_root;
    public static String pref_page_projects_settings_label_not_use_hidden_folders;
    public static String pref_page_projects_settings_label_not_store_resources_in_another_project;
    public static String pref_page_projects_settings_label_restart_require_refresh_global_settings;
    public static String pref_page_projects_settings_description;

    public static String ui_navigator_loading_text_loading;
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

    public static String editors_entity_read_expensive_props_action;
    public static String editors_entity_dialog_persist_title;
    public static String editors_entity_dialog_preview_title;
    public static String editors_entity_monitor_add_folder;
    public static String editors_entity_monitor_add_node;
    public static String editors_entity_monitor_preview_changes;
    public static String editors_entity_properties_text;
    public static String editors_entity_properties_tooltip_suffix;
    public static String editors_entity_title_uninitialized;
    public static String editors_entity_title_initializing;

    public static String confirm_entity_delete_title;
    public static String confirm_entity_delete_message;
    public static String confirm_deleting_dependent_objects_title;
    public static String confirm_deleting_dependent_objects;
    public static String confirm_deleting_dependent_one_object;
    public static String search_dependencies_error_title;
    public static String search_dependencies_error_message;

    public static String confirm_local_folder_delete_title;
    public static String confirm_local_folder_delete_message;

    public static String label_configure_columns;

    public static String label_description;

    public static String label_name;

    public static String label_select_columns;

    public static String label_show_all_projects;
    public static String label_show_connected;

    public static String pref_page_target_button_use_datasource_settings;
    public static String pref_page_target_link_show_datasource_settings;
    public static String pref_page_target_link_show_global_settings;

    public static String label_active_service_instance;
    public static String label_error_list;
    public static String label_instance;

    public static String error_deleting_multiple_objects_from_different_datasources_title;
    public static String error_deleting_multiple_objects_from_different_datasources_message;
    public static String confirm_deleting_multiple_objects_title;
    public static String confirm_deleting_multiple_objects_message;
    public static String confirm_deleting_multiple_objects_table_group_name;
    public static String confirm_deleting_multiple_objects_column_name;
    public static String confirm_deleting_multiple_objects_column_description;
    public static String confirm_deleting_delete_cascade_checkbox_label;
    public static String confirm_deleting_delete_cascade_checkbox_tooltip;
    public static String confirm_deleting_close_existing_connections_checkbox_label;
    public static String confirm_deleting_close_existing_connections_checkbox_tooltip;
    public static String confirm_deleting_delete_contents_checkbox;
    public static String confirm_deleting_delete_contents_checkbox_tooltip;
    public static String confirm_deleting_project_location_label;
    public static String error_deleting_resource_title;
    public static String error_deleting_resource_message;
    public static String question_no_sql_available;
    public static String error_sql_generation_title;
    public static String error_sql_generation_message;

    public static String navigator_expand_all_text;
    public static String navigator_expand_all_tip;
    public static String navigator_project_explorer_columns_name_text;
    public static String navigator_project_explorer_columns_name_description;
    public static String navigator_project_explorer_columns_datasource_text;
    public static String navigator_project_explorer_columns_datasource_description;
    public static String navigator_project_explorer_columns_preview_text;
    public static String navigator_project_explorer_columns_preview_description;
    public static String navigator_project_explorer_columns_size_text;
    public static String navigator_project_explorer_columns_size_description;
    public static String navigator_project_explorer_columns_modified_text;
    public static String navigator_project_explorer_columns_modified_description;
    public static String navigator_project_explorer_columns_type_text;
    public static String navigator_project_explorer_columns_type_description;
    public static String navigator_filtered_nodes_text;
    public static String navigator_filtered_nodes_tip;

    public static String datasource_list_action_empty;
    public static String filter_connection_name_placeholder;

    public static String failed_to_paste_due_to_permissions_title;
    public static String failed_to_paste_due_to_permissions_message;

    public static String object_list_control_group_by_label;
    public static String object_list_control_clear_grouping_label;
    public static String pref_page_project_resource_settings_description;
    public static String navigator_handler_connections_filter_show_all_text;
    public static String navigator_handler_connections_filter_show_connected_text;
    public static String navigator_handler_object_create_file_other_text;

    public static String navigator_provider_element_tooltip_datasource_name;
    public static String navigator_provider_element_tooltip_datasource_url;
    public static String navigator_provider_element_tooltip_datasource_database_name;
    public static String navigator_provider_element_tooltip_datasource_database_version;
    public static String navigator_provider_element_tooltip_datasource_user;
    public static String navigator_provider_element_tooltip_datasource_description;
    public static String navigator_provider_element_tooltip_datasource_read_only;
    public static String navigator_provider_element_tooltip_datasource_provided;
    public static String navigator_provider_element_tooltip_datasource_error;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UINavigatorMessages.class);
    }

    private UINavigatorMessages() {
    }
}
