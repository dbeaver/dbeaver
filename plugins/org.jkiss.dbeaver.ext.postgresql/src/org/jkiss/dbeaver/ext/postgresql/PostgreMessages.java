/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 * Copyright (C) 2017 Liu, Yuanyuan (liuyuanyuan@highgo.com)
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
package org.jkiss.dbeaver.ext.postgresql;

import org.eclipse.osgi.util.NLS;

public class PostgreMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.postgresql.PostgresResources"; //$NON-NLS-1$

    public static String procedure_warning_name;
    public static String procedure_warning_text;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, PostgreMessages.class);
    }

    private PostgreMessages() {
    }

}
