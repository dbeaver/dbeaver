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
package org.jkiss.dbeaver.ext.duckdb.ui.internal;

import org.eclipse.osgi.util.NLS;

public class DuckDBMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.duckdb.ui.internal.DuckDBResources"; //$NON-NLS-1$
    public static String dialog_setting_sql;
    public static String dialog_setting_sql_dd_plain_label;
    public static String dialog_setting_sql_dd_tag_label;
    public static String dialog_setting_sql_dd_string;
    public static String dialog_setting_sql_dd_code_block;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, DuckDBMessages.class);
    }

    private DuckDBMessages() {
    }
}
