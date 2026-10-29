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
package org.jkiss.dbeaver.ext.cloudflare.d1.internal;

import org.eclipse.osgi.util.NLS;

public class CloudflareD1ModelMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.cloudflare.d1.internal.CloudflareD1ModelResources";
    public static String task_administration;
    public static String value_none;
    public static String error_cancelled;
    public static String error_timeout;
    public static String error_authentication;
    public static String error_permission;
    public static String error_not_found;
    public static String error_rate_limit;
    public static String error_request;

    private CloudflareD1ModelMessages() {
    }

    static {
        NLS.initializeMessages(BUNDLE_NAME, CloudflareD1ModelMessages.class);
    }
}
