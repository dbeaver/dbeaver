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
package org.jkiss.dbeaver.ext.cdata.ui.internal;

import org.eclipse.osgi.util.NLS;

public final class CDataUIMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.cdata.ui.internal.CDataUIResources";

    public static String activation_dialog_title;
    public static String activation_dialog_message;
    public static String activation_dialog_expired_message;
    public static String activation_driver;
    public static String activation_name;
    public static String activation_email;
    public static String activation_type;
    public static String activation_trial;
    public static String activation_purchased;
    public static String activation_product_key;
    public static String activation_eula_consent;
    public static String activation_buy_link;
    public static String activation_support_link;
    public static String activation_required_fields;
    public static String activation_invalid_email;
    public static String activation_failed;
    public static String auth_native_url_builder_description;
    public static String auth_native_url_builder_run;
    public static String auth_native_url_builder_job;
    public static String auth_native_url_builder_error;
    public static String auth_native_url_builder_exit_code;
    public static String license_group_title;
    public static String license_status;
    public static String license_manage;
    public static String license_activate;
    public static String license_status_not_installed;
    public static String license_status_trial_active;
    public static String license_status_trial_expiring;
    public static String license_status_trial_expired;
    public static String license_status_purchased_active;
    public static String license_status_purchased_expiring;
    public static String license_status_expired;
    public static String license_status_invalid_key;
    public static String license_status_machine_mismatch;
    public static String license_status_wrong_major;
    public static String license_status_validation_unavailable;
    public static String preference_description;
    public static String preference_column_driver;
    public static String preference_column_version;
    public static String preference_column_type;
    public static String preference_column_status;
    public static String preference_column_expiration;
    public static String preference_type_trial;
    public static String preference_type_purchased;
    public static String preference_type_unknown;
    public static String preference_expiration_days;
    public static String preference_expiration_expired;
    public static String preference_expiration_unavailable;
    public static String preference_loading;
    public static String preference_no_licenses;
    public static String preference_license_count;
    public static String preference_add_key;
    public static String preference_refresh;
    public static String preference_service_unavailable;
    public static String preference_load_job;

    static {
        NLS.initializeMessages(BUNDLE_NAME, CDataUIMessages.class);
    }

    private CDataUIMessages() {
    }
}
