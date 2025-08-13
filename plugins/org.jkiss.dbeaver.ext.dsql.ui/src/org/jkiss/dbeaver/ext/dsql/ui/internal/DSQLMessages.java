/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.dsql.ui.internal;

import org.jkiss.dbeaver.utils.NLS;

public class DSQLMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.dsql.ui.internal.DSQLResources";

    public static String label_dsql_endpoint;

    public static String label_username;
    public static String label_use_credentials;

    public static String radio_aws_profile;
    public static String radio_aws_credentials;
    public static String radio_password;

    public static String label_aws_access_key;
    public static String label_aws_secret_key;
    public static String label_aws_session_token;
    public static String label_aws_profile;
    public static String label_dsql_token;

    public static String label_aws_region;

    static {
        NLS.initializeMessages(BUNDLE_NAME, DSQLMessages.class);
    }

    private DSQLMessages() {}
}
