/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.sqlite.ui.internal;

import org.eclipse.osgi.util.NLS;

public class SQLiteUIMessages extends NLS {
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.sqlite.ui.internal.SQLiteUIMessages"; //$NON-NLS-1$

    static {
        NLS.initializeMessages(BUNDLE_NAME, SQLiteUIMessages.class);
    }

    public static String page_extensions_title;
    public static String page_extensions_description;
    public static String page_extensions_toolbar_add;
    public static String page_extensions_toolbar_remove;
    public static String page_extensions_chooser_title;
    public static String page_extensions_chooser_name;
    public static String page_extensions_tip;
}
