/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.nativetool.internal;

import org.eclipse.osgi.util.NLS;

public class TaskNativeUIMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.tasks.ui.nativetool.internal.TaskNativeUIMessages"; //$NON-NLS-1$

    public static String tools_script_execute_wizard_task_completed;
    public static String tools_wizard_error_task_error_message;
    public static String tools_wizard_error_task_error_title;
    public static String tools_wizard_error_no_database_object_selected;
    public static String tools_wizard_error_no_native_client_selected;
    public static String tools_wizard_database_group_title;
    public static String tools_wizard_client_group_title;
    public static String tools_wizard_client_group_client;
    public static String tools_wizard_client_button;
    public static String tools_wizard_client_dialog_title;
    public static String tools_wizard_error_task_canceled;
    public static String tools_wizard_log_process_exit_code;
    public static String tools_wizard_log_io_error;
    public static String tools_wizard_message_client_home_not_found;
    public static String tools_wizard_message_no_client_home;
    public static String tools_wizard_page_log_task_finished;
    public static String tools_wizard_page_log_task_log_reader;
    public static String tools_wizard_page_log_task_progress;
    public static String tools_wizard_page_log_task_progress_log;
    public static String tools_wizard_page_log_task_started_at;
    public static String tools_db_export_wizard_file_already_exists_title;
    public static String tools_db_export_wizard_file_already_exists_message;
    public static String tools_db_export_wizard_file_have_not_been_deleted_title;
    public static String tools_db_export_wizard_file_have_not_been_deleted_message;

    public static String tools_wizard_page_dialog_button_all;
    public static String tools_wizard_page_dialog_button_none;
    public static String tools_wizard_page_dialog_label_extra_command_args;
    public static String tools_wizard_page_dialog_tooltip_extra_command_args;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, TaskNativeUIMessages.class);
    }

    private TaskNativeUIMessages() {
    }
}
