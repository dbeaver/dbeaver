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
    public static String sync_preference_page_description;
    public static String sync_preference_page_authentication_group;
    public static String sync_preference_page_about_link;
    public static String sync_preference_page_encryption_keys_group;
    public static String sync_preference_page_unknown;
    public static String sync_preference_page_account_label;
    public static String sync_preference_page_account_signed_in;
    public static String sync_preference_page_account_signed_in_as;
    public static String sync_preference_page_account_not_signed_in;
    public static String sync_preference_page_desktop_sign_in;
    public static String sync_preference_page_refresh_token;
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
    public static String project_sync_update_success;
    public static String project_sync_update_conflict;
    public static String project_sync_update_failed;
    public static String project_sync_resolve_job;
    public static String project_sync_resolve_failed;
    public static String project_sync_revisions_job;
    public static String project_sync_revisions_failed;
    public static String project_sync_conflict_dialog_title;
    public static String project_sync_conflict_heading;
    public static String project_sync_conflict_project;
    public static String project_sync_conflict_version;
    public static String project_sync_conflict_new_revisions;
    public static String project_sync_conflict_last_revision;
    public static String project_sync_conflict_last_sync;
    public static String project_sync_conflict_server_version;
    public static String project_sync_conflict_replaces;
    public static String project_sync_conflict_use_local;
    public static String project_sync_conflict_use_server;
    public static String project_sync_conflict_stop;
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
    public static String project_sync_import_list_failed;
    public static String project_sync_import_job;
    public static String project_sync_import_failed;
    public static String project_edit_job;
    public static String project_edit_failed;

    public static String projects_show_remote;
    public static String projects_page_description;
    public static String projects_page_about_link;
    public static String projects_page_sign_in_help;
    public static String projects_page_sync_help;
    public static String projects_name;
    public static String projects_description;
    public static String projects_status;
    public static String projects_last_revision;
    public static String projects_edit;
    public static String projects_sync;
    public static String projects_download;
    public static String projects_disconnect;
    public static String projects_unbind_selected_confirm_message;
    public static String projects_connect;
    public static String projects_sync_all;
    public static String projects_import_all;
    public static String projects_not_sync;
    public static String projects_choose_local;
    public static String projects_connect_failed;
    public static String projects_not_on_device;
    public static String projects_missing_on_server;
    public static String projects_up_to_date;
    public static String projects_local_changes;
    public static String projects_server_changes;
    public static String projects_both_changes;
    public static String projects_errors;
    public static String projects_conflicts;

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
