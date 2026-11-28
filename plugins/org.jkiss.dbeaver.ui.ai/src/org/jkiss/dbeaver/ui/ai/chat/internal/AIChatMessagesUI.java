/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.chat.internal;

import org.eclipse.osgi.util.NLS;

public class AIChatMessagesUI extends NLS {
    private static final String BUNDLE_NAME = AIChatMessagesUI.class.getName();

    public static String ai_chat_connect_error_title;
    public static String ai_chat_connect_error_message;
    public static String ai_chat_settings_save_title;
    public static String ai_chat_settings_save_error;
    public static String ai_chat_query_execution_error;
    public static String ai_chat_insert_sql_title;
    public static String ai_chat_insert_sql_error;
    public static String ai_chat_open_editor_error;
    public static String ai_chat_active_connection_required;
    public static String ai_chat_set_data_source_error;
    public static String ai_chat_toggle_title;
    public static String ai_chat_open_error;
    public static String ai_chat_file_open_error_title;
    public static String ai_chat_file_open_error_message;
    public static String ai_chat_conversation_create_error_title;
    public static String ai_chat_conversation_create_error_message;
    public static String ai_chat_conversation_delete_error_title;
    public static String ai_chat_conversation_delete_error_message;
    public static String ai_chat_file_not_found_title;
    public static String ai_chat_file_not_found_message;
    public static String ai_chat_file_explorer_error;
    public static String ai_chat_conversation_update_error_title;
    public static String ai_chat_conversation_update_error_message;
    public static String ai_chat_context_load_error_title;
    public static String ai_chat_context_load_error_message;
    public static String ai_chat_data_source_set_error_title;
    public static String ai_chat_data_source_set_error_message;
    public static String ai_chat_data_source_clear_error_title;
    public static String ai_chat_data_source_clear_error_message;
    public static String ai_chat_conversation_select_error_title;
    public static String ai_chat_conversation_select_error_message;

    public static String ai_chat_message_role_user_label;
    public static String ai_chat_message_role_assistant_label;
    public static String ai_chat_message_role_function_label;
    public static String ai_chat_message_role_system_label;
    public static String ai_chat_message_role_generated_label;
    public static String ai_chat_message_role_warning_label;
    public static String ai_chat_message_role_error_label;
    public static String ai_chat_message_copy_to_clipboard_tip;
    public static String ai_chat_message_copy_to_editor_tip;
    public static String ai_chat_message_execute_query_tip;
    public static String ai_chat_message_clear_up_to_here_tip;
    public static String ai_chat_message_delete_message_tip;
    public static String ai_chat_message_meta_tokens;
    public static String ai_chat_message_meta_tokens_unavailable;
    public static String ai_chat_ai_notice;
    public static String ai_chat_prompt_text_hint;
    public static String ai_chat_send_button_tip;
    public static String ai_chat_cancel_button_tip;
    public static String ai_chat_change_scope_label;
    public static String ai_chat_settings_label;
    public static String ai_chat_clear_history_confirm_title;
    public static String ai_chat_clear_history_partial_confirm_message;
    public static String ai_chat_empty_hint;
    public static String ai_chat_drag_n_drop_message;
    public static String ai_chat_drag_n_drop_objects_message;
    public static String ai_chat_default_conversation_name;
    public static String ai_chat_attach_button_tip;
    public static String ai_chat_confirm_title;
    public static String ai_chat_confirm_title_prefix;
    public static String ai_chat_confirm_title_middle;
    public static String ai_chat_confirm_approve;
    public static String ai_chat_confirm_deny;
    public static String ai_chat_confirm_auto_approved;
    public static String ai_chat_confirm_failed;
    public static String ai_chat_confirm_approved;
    public static String ai_chat_confirm_declined;
    public static String ai_chat_confirm_params;
    public static String ai_chat_confirm_result;
    public static String ai_chat_confirm_dangerous;
    public static String ai_chat_confirm_allow_options;
    public static String ai_chat_attachment_invalid_data;
    public static String ai_chat_attachment_unknown_file;
    public static String ai_chat_attachment_show_in_explorer;
    public static String ai_chat_attachment_create_error;
    public static String ai_chat_attachment_choose_type;
    public static String ai_chat_attachment_choose_type_tip;
    public static String ai_chat_attachment_no_handlers;
    public static String ai_chat_attachment_no_actions;
    public static String ai_chat_attachment_no_type_actions;
    public static String ai_chat_attachment_open_as_table;
    public static String ai_chat_attachment_import_file;
    public static String ai_chat_attachment_import_files;
    public static String ai_chat_attachment_open_completed;
    public static String ai_chat_attachment_import_completed;
    public static String ai_chat_message_role_attachment_label;
    public static String ai_chat_conversation_new_label;
    public static String ai_chat_conversation_delete_label;
    public static String ai_chat_conversation_delete_confirm_title;
    public static String ai_chat_conversation_delete_confirm_message;
    public static String ai_chat_a11y_transcript_label;
    public static String ai_chat_a11y_prompt_name;
    public static String ai_chat_a11y_conversation_name;
    public static String ai_chat_a11y_connection_name;
    public static String ai_chat_a11y_waiting_for_response;
    public static String ai_chat_show_profile_and_model;
    public static String ai_chat_profile_label;
    public static String ai_chat_model_label;
    public static String ai_chat_profile_not_configured;
    public static String ai_chat_model_not_configured;
    public static String ai_chat_model_unavailable;
    public static String ai_chat_model_loading;
    public static String ai_chat_initialization_error;
    public static String ai_chat_initialization_error_message;
    public static String ai_chat_command_error_message;
    public static String ai_chat_model_change_error;
    public static String ai_chat_context_menu_no_active_connections;
    public static String ai_chat_context_menu_no_connection;
    public static String ai_chat_context_menu_no_database_connection;
    public static String ai_chat_context_menu_select_connection;
    public static String ai_chat_context_menu_applies_to;
    public static String ai_chat_context_menu_this_connection;
    public static String ai_chat_context_menu_this_conversation;
    public static String ai_chat_context_menu_database_connecting;
    public static String ai_chat_context_menu_database_not_connected;
    public static String ai_chat_context_menu_metadata_sent;
    public static String ai_chat_context_menu_active_configuration;
    public static String ai_chat_context_menu_messages;
    public static String ai_chat_context_menu_show_message_time;
    public static String ai_chat_context_menu_show_time_spent;
    public static String ai_chat_context_menu_show_tokens_spent;
    public static String ai_chat_context_menu_chat;
    public static String ai_chat_context_menu_show_total_tokens_spent;
    public static String ai_chat_context_menu_no_schema_selected;
    public static String ai_chat_context_menu_no_database_selected;
    public static String ai_chat_context_menu_all_objects;
    public static String ai_chat_context_menu_empty;


    public static String ai_chat_image_open;
    public static String ai_chat_image_remove;
    public static String ai_chat_image_error;
    public static String ai_chat_image_load_error;
    public static String ai_chat_image_view_error;
    public static String ai_chat_image_limit;
    public static String ai_chat_image_loading;
    public static String ai_chat_image_clipboard_name;

    static {
        NLS.initializeMessages(BUNDLE_NAME, AIChatMessagesUI.class);
    }
}
