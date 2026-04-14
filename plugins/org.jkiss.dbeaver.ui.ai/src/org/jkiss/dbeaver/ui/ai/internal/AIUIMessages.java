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
package org.jkiss.dbeaver.ui.ai.internal;

import org.eclipse.osgi.util.NLS;

public class AIUIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.ai.internal.AIUIMessages"; //$NON-NLS-1$

    public static String gpt_preference_page_checkbox_enable_ai_label;
    public static String gpt_preference_page_checkbox_enable_ai_tip;
    public static String gpt_preference_page_selector_base_url;
    public static String gpt_preference_page_selector_token;
    public static String gpt_preference_page_token_info;
    public static String gpt_preference_page_completion_group;
    public static String gpt_preference_page_completion_include_source_label;
    public static String gpt_preference_page_completion_include_source_tip;
    public static String gpt_preference_page_completion_execute_immediately_label;
    public static String gpt_preference_page_completion_execute_immediately_tip;
    public static String gpt_preference_page_completion_send_type_label;
    public static String gpt_preference_page_completion_send_type_tip;
    public static String gpt_preference_page_completion_execute_description_label;
    public static String gpt_preference_page_completion_execute_description_tip;
    public static String gpt_preference_page_schema_group;
    public static String gpt_preference_page_advanced_appearance_group;
    public static String gpt_preference_page_combo_engine;
    public static String gpt_preference_page_text_temperature;
    public static String gpt_preference_page_refresh_models;
    public static String gpt_preference_page_ai_query_confirm_group;
    public static String gpt_preference_page_ai_query_confirm_label;
    public static String gpt_preference_page_ai_query_confirm_tip;
    public static String gpt_preference_page_ai_query_confirm_rule_execute;
    public static String gpt_preference_page_ai_query_confirm_rule_confirm;
    public static String gpt_preference_page_ai_query_confirm_rule_disable_autocommit;

    public static String gpt_preference_page_ai_query_confirm_sql_label;
    public static String gpt_preference_page_ai_query_confirm_sql_tip;
    public static String gpt_preference_page_ai_query_confirm_dml_label;
    public static String gpt_preference_page_ai_query_confirm_dml_tip;
    public static String gpt_preference_page_ai_query_confirm_ddl_label;
    public static String gpt_preference_page_ai_query_confirm_ddl_tip;
    public static String gpt_preference_page_ai_query_confirm_other_label;
    public static String gpt_preference_page_ai_query_confirm_other_tip;

    public static String gpt_preference_page_ai_connection_test_label;
    public static String gpt_preference_page_ai_connection_test_connection_success_title;
    public static String gpt_preference_page_ai_connection_test_connection_success_message;
    public static String gpt_preference_page_ai_connection_test_connection_error_title;
    public static String gpt_preference_page_ai_connection_test_connection_error_message;

    public static String gpt_preference_page_prompts_hint;

    public static String confirm_meta_transfer_usage_title;
    public static String confirm_meta_transfer_usage_message;

    public static String ai_suggestion_popup_title;
    public static String ai_suggestion_popup_message;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, AIUIMessages.class);
    }

    private AIUIMessages() {
    }
}
