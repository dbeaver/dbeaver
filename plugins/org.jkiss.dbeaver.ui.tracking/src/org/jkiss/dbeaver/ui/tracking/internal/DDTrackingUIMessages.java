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
package org.jkiss.dbeaver.ui.tracking.internal;

import org.eclipse.osgi.util.NLS;

public class DDTrackingUIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.tracking.internal.DDTrackingUIMessages"; //$NON-NLS-1$

    public static String sync_preference_page_title;
    public static String sync_preference_page_access_key_group;
    public static String sync_preference_page_server_url_label;
    public static String sync_preference_page_default_button;
    public static String sync_preference_page_account_label;
    public static String sync_preference_page_log_in_button;
    public static String sync_preference_page_log_out_button;
    public static String sync_preference_page_log_out_confirm_title;
    public static String sync_preference_page_log_out_confirm_message;
    public static String sync_preference_page_configuration_group;
    public static String sync_preference_page_bound_to_label;
    public static String sync_preference_page_upload_button;
    public static String sync_preference_page_download_button;
    public static String sync_preference_page_download_options_button;
    public static String sync_preference_page_nothing_to_upload;
    public static String sync_preference_page_uploaded_label;
    public static String sync_preference_page_upload_conflict;
    public static String sync_preference_page_upload_failed;
    public static String sync_preference_page_nothing_bound;
    public static String sync_preference_page_nothing_to_download;
    public static String sync_preference_page_downloaded_label;
    public static String sync_preference_page_download_conflict;
    public static String sync_preference_page_configuration_not_found;
    public static String sync_preference_page_download_failed;
    public static String sync_preference_page_no_configurations_found;
    public static String sync_preference_page_select_configuration;
    public static String sync_preference_page_log_in_first;
    public static String sync_preference_page_url_not_configured;
    public static String sync_preference_page_encryption_not_configured;
    public static String sync_preference_page_login_failed;
    public static String sync_preference_page_cannot_forget_keys;
    public static String sync_preference_page_auto_sync_checkbox;
    public static String sync_preference_page_conflicts_label;
    public static String sync_preference_page_take_remote_button;
    public static String sync_preference_page_keep_local_button;
    public static String sync_preference_page_conflict_resolved_label;
    public static String sync_preference_page_conflict_resolve_failed;

    public static String create_configuration_dialog_title;
    public static String create_configuration_dialog_name_label;
    public static String create_configuration_dialog_include_label;
    public static String create_configuration_dialog_create_button;

    public static String import_key_dialog_title;
    public static String import_key_dialog_prompt_label;
    public static String import_key_dialog_paste_button;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, DDTrackingUIMessages.class);
    }

    private DDTrackingUIMessages() {
    }
}
