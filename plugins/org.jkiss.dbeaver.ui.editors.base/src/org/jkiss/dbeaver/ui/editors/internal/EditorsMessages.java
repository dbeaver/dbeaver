/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.internal;

import org.jkiss.dbeaver.utils.NLS;

public class EditorsMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.internal.EditorsResources"; //$NON-NLS-1$

    public static String dialog_morph_delimited_shell_text;
    public static String dialog_morph_delimited_source_group;
    public static String dialog_morph_delimited_source_group_delimiter;
    public static String dialog_morph_delimited_target_group_label;
    public static String dialog_morph_delimited_target_group_delim_result;
    public static String dialog_morph_delimited_target_group_delim_quote;
    public static String dialog_morph_delimited_target_group_spinner_wrap_line;
    public static String dialog_morph_delimited_target_group_spinner_wrap_line_tip;
    public static String dialog_morph_delimited_target_group_leading_text;
    public static String dialog_morph_delimited_target_group_trailing_text;

    public static String database_editor_command_save_name;
    public static String database_editor_command_save_tip;
    public static String database_editor_command_revert_name;
    public static String database_editor_command_revert_tip;
    public static String database_editor_command_refresh_name;
    public static String database_editor_command_refresh_tip;

    public static String database_editor_project;

    public static String file_dialog_select_files;
    public static String file_dialog_save_failed;
    public static String file_dialog_save_as_file;
    public static String file_dialog_cannot_load_file;

    public static String progress_editor_initializing_text;
    public static String progress_editor_uninitialized_text;

    public static String lazy_editor_input_cant_find_node;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, EditorsMessages.class);
    }

    private EditorsMessages() {
    }
}
