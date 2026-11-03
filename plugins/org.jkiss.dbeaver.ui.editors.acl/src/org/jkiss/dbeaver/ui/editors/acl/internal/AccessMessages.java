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
package org.jkiss.dbeaver.ui.editors.acl.internal;

import org.eclipse.osgi.util.NLS;

public class AccessMessages extends NLS {
    public static String access_revoke_permission;
    public static String access_role;
    public static String access_edit;
    public static String access_cascade_tip;
    public static String access_scope;
    public static String access_mapping;
    public static String access_user_name;
    public static String access_default_schema;
    public static String access_roles;
    public static String access_permissions;
    public static String access_explicit_permissions;
    public static String access_securable;
    public static String access_permission;
    public static String access_state;
    public static String access_grantor;
    public static String access_add;
    public static String access_revoke;
    public static String access_load;
    public static String access_change;
    public static String access_grant;
    public static String access_grant_option;
    public static String access_deny;
    public static String access_filter;
    public static String access_cascade;

    static {
        NLS.initializeMessages("org.jkiss.dbeaver.ui.editors.acl.internal.AccessMessages", AccessMessages.class);
    }

    private AccessMessages() {
    }
}
