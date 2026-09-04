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
package com.dbeaver.db.cdata.ui.internal;

import org.eclipse.osgi.util.NLS;

public final class CDataUIMessages extends NLS {
    private static final String BUNDLE_NAME = "com.dbeaver.db.cdata.ui.internal.CDataUIResources";

    public static String activation_dialog_title;
    public static String activation_dialog_message;
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
    public static String license_group_title;
    public static String license_status;
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

    static {
        NLS.initializeMessages(BUNDLE_NAME, CDataUIMessages.class);
    }

    private CDataUIMessages() {
    }
}
