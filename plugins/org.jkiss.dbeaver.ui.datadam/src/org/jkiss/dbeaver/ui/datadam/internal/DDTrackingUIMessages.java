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
package org.jkiss.dbeaver.ui.datadam.internal;

import org.eclipse.osgi.util.NLS;

public class DDTrackingUIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages"; //$NON-NLS-1$

    public static String sync_preference_page_title;
    public static String sync_preference_page_account_group;
    public static String sync_preference_page_encryption_keys_group;
    public static String sync_preference_page_user_label;
    public static String sync_preference_page_unknown;
    public static String sync_preference_page_account_label;
    public static String sync_preference_page_desktop_sign_in;
    public static String sync_preference_page_desktop_sign_in_again;
    public static String sync_preference_page_desktop_sign_out;
    public static String sync_preference_page_import_keys_button;
    public static String sync_preference_page_forget_keys_button;
    public static String sync_preference_page_forget_keys_confirm_title;
    public static String sync_preference_page_forget_keys_confirm_message;
    public static String sync_preference_page_import_keys_failed;
    public static String sync_preference_page_encryption_not_configured;
    public static String sync_preference_page_login_failed;
    public static String sync_preference_page_logout_failed;
    public static String sync_preference_page_cannot_forget_keys;

    public static String desktop_relogin_title;
    public static String desktop_relogin_message;

    public static String project_sync_title;
    public static String project_sync_share_job;
    public static String project_sync_share_success;
    public static String project_sync_share_failed;
    public static String project_sync_update_job;
    public static String project_sync_update_unchanged;
    public static String project_sync_update_conflict;
    public static String project_sync_update_failed;
    public static String project_sync_push_success;
    public static String project_sync_push_failed;
    public static String project_sync_pull_success;
    public static String project_sync_pull_failed;
    public static String project_sync_unbind_confirm_title;
    public static String project_sync_unbind_confirm_message;
    public static String project_sync_unbind_job;
    public static String project_sync_unbind_success;
    public static String project_sync_unbind_failed;
    public static String project_sync_import_list_job;
    public static String project_sync_import_dialog_title;
    public static String project_sync_import_dialog_message;
    public static String project_sync_import_button;
    public static String project_sync_import_no_projects;
    public static String project_sync_import_list_failed;
    public static String project_sync_import_job;
    public static String project_sync_import_success;
    public static String project_sync_import_failed;

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
