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
package org.jkiss.dbeaver.ui.editors.object.internal;

import org.jkiss.dbeaver.utils.NLS;

public class ObjectEditorMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.object.internal.ObjectEditorMessages"; //$NON-NLS-1$

    public static String dialog_struct_attribute_edit_page_header_edit_attribute;
    public static String dialog_struct_label_text_name;
    public static String dialog_struct_label_text_properties;
    public static String dialog_struct_unique;
    public static String dialog_struct_columns_select_group_columns;
    public static String dialog_struct_columns_select_label_table;
    public static String dialog_struct_columns_select_title;
    public static String dialog_struct_create_entity_group_name;
    public static String dialog_struct_create_entity_title;
    public static String dialog_struct_create_column_order;
    public static String dialog_struct_create_column_order_tooltip;
    public static String dialog_struct_create_procedure_combo_type;
    public static String dialog_struct_create_procedure_label_name;
    public static String dialog_struct_create_procedure_title;
    public static String dialog_struct_create_procedure_container;
    public static String dialog_struct_create_sequence_title;
    public static String dialog_struct_create_sequence_name;
    public static String dialog_struct_create_sequence_container;
    public static String dialog_struct_edit_constrain_label_name;
    public static String dialog_struct_edit_constrain_label_type;
    public static String dialog_struct_edit_fk_column_col_type;
    public static String dialog_struct_edit_fk_column_column;
    public static String dialog_struct_edit_fk_column_ref_col;
    public static String dialog_struct_edit_fk_column_ref_col_type;
    public static String dialog_struct_edit_fk_combo_on_delete;
    public static String dialog_struct_edit_fk_combo_on_update;
    public static String dialog_struct_edit_fk_combo_unik;
    public static String dialog_struct_edit_fk_name;
    public static String dialog_struct_edit_fk_error_load_constraint_columns_message;
    public static String dialog_struct_edit_fk_error_load_constraint_columns_title;
    public static String dialog_struct_edit_fk_error_load_constraints_message;
    public static String dialog_struct_edit_fk_error_load_constraints_title;
    public static String dialog_struct_edit_fk_label_columns;
    public static String dialog_struct_edit_fk_label_ref_table;
    public static String dialog_struct_edit_fk_label_table;
    public static String dialog_struct_edit_fk_title;
    public static String dialog_struct_edit_fk_virtual_page_title;
    public static String dialog_struct_edit_fk_page_title;
    public static String dialog_struct_edit_fk_custom_uk_button_create;
    public static String dialog_struct_edit_fk_label_key_type;
    public static String dialog_struct_edit_index_label_type;

    public static String dialog_struct_edit_dictionary_tip;
    public static String dialog_struct_edit_dictionary_custom_criteria;
    public static String dialog_struct_edit_dictionary_custom_criteria_tip;
    public static String dialog_struct_edit_dictionary_column_delimiter;
    public static String dialog_struct_edit_dictionary_column_delimiter_tip;

    public static String dialog_struct_columns_select_column;
    public static String dialog_struct_columns_type;


    public static String edit_constraints_error_title;
    public static String edit_constraints_error_message;
    public static String edit_constraints_enable_constraint_text;
    public static String edit_constraints_enable_constraint_tip;
    public static String edit_constraints_use_all_columns_text;
    public static String edit_constraints_use_all_columns_tip;
    public static String edit_constraints_expression_text;
    public static String edit_foreign_key_page_create_schema_container;
    public static String edit_foreign_key_page_create_container_reference_table_container;
    public static String edit_foreign_key_page_create_container_select_reference_table_container;
    public static String edit_foreign_key_page_error_loading_table_title;
    public static String edit_foreign_key_page_error_loading_table_message;
    public static String selector_select_all_text;
    public static String selector_clear_all_text;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, ObjectEditorMessages.class);
    }

    private ObjectEditorMessages() {
    }
}
