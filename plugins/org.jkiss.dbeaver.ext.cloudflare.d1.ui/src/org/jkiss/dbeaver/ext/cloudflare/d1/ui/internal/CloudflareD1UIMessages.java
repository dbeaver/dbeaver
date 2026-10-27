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
package org.jkiss.dbeaver.ext.cloudflare.d1.ui.internal;

import org.eclipse.osgi.util.NLS;

public class CloudflareD1UIMessages extends NLS {
    public static String connection_authentication;
    public static String connection_database;
    public static String connection_account_id;
    public static String connection_api_token;
    public static String connection_load;
    public static String connection_help;
    public static String connection_select_database;
    public static String connection_insecure;
    public static String connection_credentials_required;
    public static String connection_loading;
    public static String connection_load_task;
    public static String connection_empty;
    public static String connection_loaded;
    public static String delete_title;
    public static String delete_confirm;
    public static String delete_uuid_required;
    public static String create_title;
    public static String create_name;
    public static String create_jurisdiction;
    public static String create_jurisdiction_help;
    public static String create_location;
    public static String create_location_help;
    public static String create_none;
    public static String create_automatic;
    public static String create_action;
    public static String create_append_scope;
    public static String create_eu;
    public static String create_fedramp;
    public static String create_wnam;
    public static String create_enam;
    public static String create_weur;
    public static String create_eeur;
    public static String create_apac;
    public static String create_oc;

    static {
        initializeMessages(CloudflareD1UIMessages.class.getName(), CloudflareD1UIMessages.class);
    }

    private CloudflareD1UIMessages() {
    }
}
